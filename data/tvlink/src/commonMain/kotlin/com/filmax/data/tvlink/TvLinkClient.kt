package com.filmax.data.tvlink

import com.filmax.core.domain.tvlink.LinkFailure
import com.filmax.core.domain.tvlink.RemoteCommand
import com.filmax.core.domain.tvlink.RemoteSession
import com.filmax.core.domain.tvlink.TvDevice
import com.filmax.core.domain.tvlink.TvRemote
import com.filmax.data.tvlink.platform.LocalDevice
import com.filmax.data.tvlink.protocol.TvLinkMessage
import com.filmax.data.tvlink.protocol.TvLinkProtocol
import com.filmax.data.tvlink.protocol.toDomain
import com.filmax.data.tvlink.protocol.toMessage
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.utils.io.core.use
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.IOException
import kotlin.concurrent.Volatile

/**
 * Пульт на телефоне. Связь живёт в собственном скоупе, а не в экране: пульт закрыли, а телевизор
 * остался подключённым — вернуться к нему можно с любого экрана.
 */
internal class TvLinkClient(
    private val account: TvLinkAccount,
    private val device: LocalDevice,
    private val dispatcher: CoroutineDispatcher,
) : TvRemote {

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val currentSession = MutableStateFlow<RemoteSession>(RemoteSession.Idle)
    override val session: StateFlow<RemoteSession> = currentSession.asStateFlow()

    @Volatile
    private var link: Job? = null

    /** Очередь команд текущей связи; у новой связи — своя, чтобы старые команды не ушли новому ТВ. */
    @Volatile
    private var outgoing: Channel<RemoteCommand>? = null

    override suspend fun connect(device: TvDevice): Boolean {
        val connected = CompletableDeferred<Boolean>()
        disconnect()
        currentSession.value = RemoteSession.Connecting(device)
        val job = scope.launch { runLink(device) { connected.complete(true) } }
        // Связь кончилась раньше, чем установилась (отказ, тайм-аут, отмена), — ждущему ответ «нет».
        job.invokeOnCompletion { connected.complete(false) }
        link = job
        return connected.await()
    }

    override fun send(command: RemoteCommand) {
        outgoing?.trySend(command)
    }

    override fun disconnect() {
        link?.cancel()
        link = null
        outgoing = null
        currentSession.value = RemoteSession.Idle
    }

    private suspend fun runLink(target: TvDevice, onConnected: () -> Unit) {
        var connected = false
        val failure = try {
            open(target) { tv ->
                connected = true
                currentSession.value = RemoteSession.Connected(tv, playback = null)
                onConnected()
            }
        } catch (error: IOException) {
            linkTrail("связь с ТВ: ${error.message}")
            if (connected) LinkFailure.Dropped else LinkFailure.Unreachable
        }
        // Связь отменили (новое подключение или «Отключиться») — состояние уже не её.
        currentCoroutineContext().ensureActive()
        outgoing = null
        currentSession.value = RemoteSession.Lost(target, failure)
    }

    /** Подключиться, пройти рукопожатие и держать связь; результат — почему она кончилась. */
    private suspend fun open(target: TvDevice, onConnected: (TvDevice) -> Unit): LinkFailure {
        val key = account.key() ?: return LinkFailure.Unreachable
        return SelectorManager(dispatcher).use { selector ->
            val socket = withTimeoutOrNull(TvLinkProtocol.CONNECT_TIMEOUT_MS) {
                aSocket(selector).tcp().connect(target.host, target.port) { keepAlive = true }
            } ?: return@use LinkFailure.Unreachable
            val connection = LinkConnection(socket)
            try {
                when (val handshake = connection.handshakeAsPhone(key, device.name)) {
                    PhoneHandshake.NoAnswer -> LinkFailure.Unreachable
                    PhoneHandshake.OtherAccount -> LinkFailure.OtherAccount
                    is PhoneHandshake.Accepted -> {
                        val commands = Channel<RemoteCommand>(Channel.BUFFERED)
                        outgoing = commands
                        onConnected(target.copy(name = handshake.tvName.ifBlank { target.name }))
                        exchange(connection, commands)
                        LinkFailure.Dropped
                    }
                }
            } finally {
                connection.close()
            }
        }
    }

    /** Команды — телевизору (а без них — ping), его снимки — в сессию; пока ТВ не замолчит. */
    private suspend fun exchange(connection: LinkConnection, commands: Channel<RemoteCommand>) = coroutineScope {
        val sender = launch {
            while (true) {
                val command = withTimeoutOrNull(TvLinkProtocol.HEARTBEAT_MS) { commands.receive() }
                connection.send(command?.toMessage() ?: TvLinkMessage.Ping)
            }
        }
        var message = connection.receive()
        while (message != null) {
            onTvMessage(message)
            message = connection.receive()
        }
        sender.cancel()
    }

    private fun onTvMessage(message: TvLinkMessage) {
        val playback = when (message) {
            is TvLinkMessage.Status -> message.toDomain()
            TvLinkMessage.Idle -> null
            else -> return
        }
        currentSession.update { session ->
            if (session is RemoteSession.Connected) session.copy(playback = playback) else session
        }
    }
}

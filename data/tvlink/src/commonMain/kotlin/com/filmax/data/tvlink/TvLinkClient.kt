package com.filmax.data.tvlink

import com.filmax.core.domain.tvlink.LinkFailure
import com.filmax.core.domain.tvlink.PlaybackStatus
import com.filmax.core.domain.tvlink.RemoteCommand
import com.filmax.core.domain.tvlink.RemoteSession
import com.filmax.core.domain.tvlink.TvDevice
import com.filmax.core.domain.tvlink.TvRemote
import com.filmax.data.tvlink.platform.LocalDevice
import com.filmax.data.tvlink.platform.ServiceBrowser
import com.filmax.data.tvlink.protocol.TvLinkProtocol
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.utils.io.core.use
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.IOException
import kotlin.concurrent.Volatile

/**
 * Пульт на телефоне. Связь живёт в собственном скоупе, а не в экране: пульт закрыли, а телевизор
 * остался подключённым — вернуться к нему можно с любого экрана. ТВ, с которым связь однажды
 * установилась, запоминается ([TvLinkMemory]), и [stayConnected] возвращает её сама ([TvLinkKeeper]):
 * раньше связь жила только в памяти процесса, и после обрыва или перезапуска ТВ выбирали заново.
 */
internal class TvLinkClient(
    private val account: TvLinkAccount,
    private val device: LocalDevice,
    private val memory: TvLinkMemory,
    browser: ServiceBrowser,
    private val dispatcher: CoroutineDispatcher,
) : TvRemote {

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val keeper = TvLinkKeeper(client = this, browser = browser)

    private val currentSession = MutableStateFlow<RemoteSession>(RemoteSession.Idle)
    override val session: StateFlow<RemoteSession> = currentSession.asStateFlow()

    override val paired: StateFlow<TvDevice?> = memory.paired

    /** Текущая связь. Меняется под [linkLock]: окно выбора ТВ и [keeper] подключаются наперегонки. */
    @Volatile
    private var link: Link? = null
    private val linkLock = Mutex()

    /** Очередь команд текущей связи; у новой связи — своя, чтобы старые команды не ушли новому ТВ. */
    @Volatile
    private var outgoing: Channel<RemoteCommand>? = null

    override suspend fun connect(device: TvDevice): Boolean = connect(device, quiet = false)

    override suspend fun stayConnected() = keeper.keep()

    override fun send(command: RemoteCommand) {
        outgoing?.trySend(command)
    }

    override fun forget() {
        memory.forget()
        dropLink()
        currentSession.value = RemoteSession.Idle
    }

    /**
     * Связь с этим ТВ уже есть или устанавливается — дождаться её, иначе начать новую. [quiet] — повтор
     * без «Подключаемся…»: пульт и так показывает, что связи нет, и не мигает на каждой попытке.
     */
    suspend fun connect(target: TvDevice, quiet: Boolean): Boolean {
        val connectedId = (currentSession.value as? RemoteSession.Connected)?.device?.id
        val current = linkLock.withLock {
            link?.takeIf { it.leadsTo(target, connectedId) } ?: startLink(target, quiet)
        }
        return current.connected.await()
    }

    private fun startLink(target: TvDevice, quiet: Boolean): Link {
        dropLink()
        if (!quiet) currentSession.value = RemoteSession.Connecting(target)
        val connected = CompletableDeferred<Boolean>()
        val job = scope.launch { runLink(target) { connected.complete(true) } }
        // Связь кончилась раньше, чем установилась (отказ, тайм-аут, отмена), — ждущему ответ «нет».
        job.invokeOnCompletion { connected.complete(false) }
        return Link(target, job, connected).also { link = it }
    }

    private fun dropLink() {
        link?.job?.cancel()
        link = null
        outgoing = null
    }

    private suspend fun runLink(target: TvDevice, onConnected: () -> Unit) {
        var linked: TvDevice? = null
        val failure = try {
            open(target) { tv ->
                linked = tv
                memory.remember(tv)
                currentSession.value = RemoteSession.Connected(tv, playback = null)
                onConnected()
            }
        } catch (error: IOException) {
            linkTrail("связь с ТВ: ${error.message}")
            if (linked != null) LinkFailure.Dropped else LinkFailure.Unreachable
        }
        // Связь отменили (новое подключение или «Забыть») — состояние уже не её.
        currentCoroutineContext().ensureActive()
        outgoing = null
        currentSession.value = RemoteSession.Lost(linked ?: target, failure)
    }

    /** Подключиться, пройти рукопожатие и держать связь; результат — почему она кончилась. */
    private suspend fun open(target: TvDevice, onConnected: (TvDevice) -> Unit): LinkFailure =
        SelectorManager(dispatcher).use { selector ->
            val socket = withTimeoutOrNull(TvLinkProtocol.CONNECT_TIMEOUT_MS) {
                aSocket(selector).tcp().connect(target.host, target.port) { keepAlive = true }
            } ?: return@use LinkFailure.Unreachable
            val connection = LinkConnection(socket)
            try {
                // Ключ — когда ТВ уже ответил: повторы к выключенному ТВ не дёргают профиль kino.pub.
                val key = account.key() ?: return@use LinkFailure.Unreachable
                when (val handshake = connection.handshakeAsPhone(key, device.name)) {
                    PhoneHandshake.NoAnswer -> LinkFailure.Unreachable
                    PhoneHandshake.OtherAccount -> LinkFailure.OtherAccount
                    is PhoneHandshake.Accepted -> {
                        val commands = Channel<RemoteCommand>(Channel.BUFFERED)
                        outgoing = commands
                        onConnected(target.linkedAs(handshake))
                        connection.exchangeAsPhone(commands, ::showPlayback)
                        LinkFailure.Dropped
                    }
                }
            } finally {
                connection.close()
            }
        }

    private fun showPlayback(playback: PlaybackStatus?) {
        currentSession.update { session ->
            if (session is RemoteSession.Connected) session.copy(playback = playback) else session
        }
    }

    /** Связь с [target]; [connected] — установилась ли она: ждут все, кто подключался к этому ТВ. */
    private class Link(val target: TvDevice, val job: Job, val connected: Deferred<Boolean>) {

        /** Подключение по адресу и запомненный id ведут к одному ТВ: сверяем и с тем, кто подключён. */
        fun leadsTo(tv: TvDevice, connectedId: String?): Boolean =
            job.isActive && (target.id == tv.id || connectedId == tv.id)
    }
}

/**
 * ТВ, каким он назвался в рукопожатии: постоянный id и имя из приветствия. ТВ на Filmax до 2.6 их
 * не присылает — остаются найденные в сети или введённый адрес.
 */
private fun TvDevice.linkedAs(handshake: PhoneHandshake.Accepted): TvDevice =
    copy(id = handshake.tvId.ifBlank { id }, name = handshake.tvName.ifBlank { name })

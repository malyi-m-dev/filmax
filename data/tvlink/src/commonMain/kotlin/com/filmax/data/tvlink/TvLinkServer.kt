package com.filmax.data.tvlink

import com.filmax.core.domain.tvlink.PlaybackStatus
import com.filmax.core.domain.tvlink.ReceiverState
import com.filmax.core.domain.tvlink.RemoteCommand
import com.filmax.core.domain.tvlink.TvLinkReceiver
import com.filmax.data.tvlink.platform.LinkAdvert
import com.filmax.data.tvlink.platform.LocalDevice
import com.filmax.data.tvlink.platform.ServiceAdvertiser
import com.filmax.data.tvlink.protocol.TvLinkProtocol
import com.filmax.data.tvlink.protocol.toCommand
import com.filmax.data.tvlink.protocol.toMessage
import com.filmax.data.tvlink.security.TvLinkKey
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.utils.io.core.use
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.IOException

/**
 * Приёмник на ТВ: слушает порт, объявляет ТВ в mDNS и пускает телефоны, прошедшие рукопожатие. Каждый
 * подключённый телефон получает снимки плеера ([playback]), его команды уходят в [commands].
 */
internal class TvLinkServer(
    private val account: TvLinkAccount,
    private val advertiser: ServiceAdvertiser,
    private val device: LocalDevice,
    private val dispatcher: CoroutineDispatcher,
) : TvLinkReceiver {

    private val receiverState = MutableStateFlow(ReceiverState())
    override val state: StateFlow<ReceiverState> = receiverState.asStateFlow()

    // Команду, которую сейчас никто не слушает (пауза при закрытом плеере), не копим: она устарела.
    private val incoming = MutableSharedFlow<RemoteCommand>(extraBufferCapacity = COMMAND_BUFFER)
    override val commands: Flow<RemoteCommand> = incoming.asSharedFlow()

    private val currentPlayback = MutableStateFlow<PlaybackStatus?>(null)
    override val playback: StateFlow<PlaybackStatus?> = currentPlayback.asStateFlow()

    override fun publish(status: PlaybackStatus?) {
        currentPlayback.value = status
    }

    override suspend fun serve() {
        val key = account.key() ?: return linkTrail("приёмник не поднят: нет профиля")
        withContext(dispatcher) {
            SelectorManager(dispatcher).use { selector ->
                bind(selector).use { server -> listen(server, key) }
            }
        }
    }

    /** Стандартный порт, а если он занят (вторая сборка Filmax на том же ТВ) — любой свободный. */
    private suspend fun bind(selector: SelectorManager): ServerSocket = try {
        aSocket(selector).tcp().bind(port = TvLinkProtocol.DEFAULT_PORT) { reuseAddress = true }
    } catch (error: IOException) {
        linkTrail("порт ${TvLinkProtocol.DEFAULT_PORT} занят: ${error.message}")
        aSocket(selector).tcp().bind(port = 0)
    }

    private suspend fun listen(server: ServerSocket, key: TvLinkKey) {
        val port = (server.localAddress as? InetSocketAddress)?.port ?: TvLinkProtocol.DEFAULT_PORT
        val address = device.localAddress()?.let { TvAddress.format(it, port) }
        receiverState.value = ReceiverState(listening = true, address = address)
        linkTrail("приёмник слушает $address")
        try {
            coroutineScope {
                launch { advertiser.advertise(LinkAdvert(device.name, port, key.fingerprint)) }
                while (isActive) {
                    val socket = server.accept()
                    launch { servePhone(socket, key) }
                }
            }
        } finally {
            receiverState.value = ReceiverState()
        }
    }

    private suspend fun servePhone(socket: Socket, key: TvLinkKey) {
        val connection = LinkConnection(socket)
        try {
            val phone = connection.handshakeAsTv(key, device.name) ?: return linkTrail("телефон не пущен")
            receiverState.update { it.copy(phones = it.phones + phone) }
            try {
                exchange(connection)
            } finally {
                receiverState.update { it.copy(phones = it.phones - phone) }
            }
        } catch (error: IOException) {
            linkTrail("телефон отключился: ${error.message}")
        } finally {
            connection.close()
        }
    }

    /** Снимки плеера — телефону, его команды — плееру; пока телефон не замолчит дольше тайм-аута. */
    private suspend fun exchange(connection: LinkConnection) = coroutineScope {
        val snapshots = launch { sendPlayback(connection) }
        var message = connection.receive()
        while (message != null) {
            message.toCommand()?.let { incoming.emit(it) }
            message = connection.receive()
        }
        snapshots.cancel()
    }

    /** Снимок — сразу на изменение и повтором раз в [TvLinkProtocol.HEARTBEAT_MS]: он же ping для пульта. */
    private suspend fun sendPlayback(connection: LinkConnection) {
        currentPlayback.collectLatest { status ->
            val message = status.toMessage()
            while (true) {
                connection.send(message)
                delay(TvLinkProtocol.HEARTBEAT_MS)
            }
        }
    }

    private companion object {
        const val COMMAND_BUFFER = 16
    }
}

package com.filmax.data.fake

import com.filmax.core.domain.tvlink.LinkFailure
import com.filmax.core.domain.tvlink.PlaybackStatus
import com.filmax.core.domain.tvlink.ReceiverState
import com.filmax.core.domain.tvlink.RemoteCommand
import com.filmax.core.domain.tvlink.RemoteSession
import com.filmax.core.domain.tvlink.TvDevice
import com.filmax.core.domain.tvlink.TvDiscovery
import com.filmax.core.domain.tvlink.TvLinkReceiver
import com.filmax.core.domain.tvlink.TvRemote
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/*
 * Связь телефона с ТВ без сети — для юнит-тестов ScreenModel'ей. Мок-сборка их не берёт: связь там
 * настоящая (data:tvlink), чтобы две мок-сборки в одной Wi-Fi находили друг друга.
 */

/** ТВ, которые «видны в сети»: тест кладёт их в [found]. */
class FakeTvDiscovery : TvDiscovery {

    val found = MutableStateFlow<List<TvDevice>>(emptyList())

    override fun devices(): Flow<List<TvDevice>> = found

    override fun deviceAt(address: String): TvDevice? = address.trim()
        .takeIf { ADDRESS.matches(it) }
        ?.let { TvDevice(id = it, name = it, host = it, port = PORT) }

    private companion object {
        val ADDRESS = Regex("[0-9.]+")
        const val PORT = 47100
    }
}

/**
 * Пульт без сети: подключается к ТВ из [reachable] и запоминает его, а ТВ из [otherAccount] отказывает,
 * как чужой аккаунт. Отправленные команды копятся в [sent], подключения — в [connects].
 */
class FakeTvRemote : TvRemote {

    val reachable = mutableSetOf<String>()
    val otherAccount = mutableSetOf<String>()
    val sent = mutableListOf<RemoteCommand>()
    val connects = mutableListOf<TvDevice>()

    /** Сколько корутин сейчас держат связь ([stayConnected]). */
    val keepers = MutableStateFlow(0)

    private val current = MutableStateFlow<RemoteSession>(RemoteSession.Idle)
    override val session: StateFlow<RemoteSession> = current.asStateFlow()

    private val pairedTv = MutableStateFlow<TvDevice?>(null)
    override val paired: StateFlow<TvDevice?> = pairedTv.asStateFlow()

    override suspend fun connect(device: TvDevice): Boolean {
        // Как и настоящий пульт: связь с этим же ТВ не рвётся ради второго такого же запроса.
        if ((current.value as? RemoteSession.Connected)?.device?.id == device.id) return true
        connects += device
        current.value = when (device.id) {
            in reachable -> RemoteSession.Connected(device, playback = null)
            in otherAccount -> RemoteSession.Lost(device, LinkFailure.OtherAccount)
            else -> RemoteSession.Lost(device, LinkFailure.Unreachable)
        }
        if (current.value is RemoteSession.Connected) pairedTv.value = device
        return current.value is RemoteSession.Connected
    }

    override suspend fun stayConnected() {
        keepers.update { it + 1 }
        try {
            awaitCancellation()
        } finally {
            keepers.update { it - 1 }
        }
    }

    override fun send(command: RemoteCommand) {
        if (current.value is RemoteSession.Connected) sent += command
    }

    override fun forget() {
        pairedTv.value = null
        current.value = RemoteSession.Idle
    }

    /** ТВ запомнен с прошлого запуска: связи с ним ещё нет. */
    fun pair(device: TvDevice) {
        pairedTv.value = device
    }

    /** ТВ прислал снимок плеера. */
    fun showPlayback(status: PlaybackStatus?) {
        current.update { session ->
            if (session is RemoteSession.Connected) session.copy(playback = status) else session
        }
    }

    /** Связь оборвалась. */
    fun drop() {
        current.update { session ->
            if (session is RemoteSession.Connected) RemoteSession.Lost(session.device, LinkFailure.Dropped) else session
        }
    }
}

/** Приёмник ТВ без сети: «слушает», пока идёт [serve]; команды телефона тест шлёт через [receive]. */
class FakeTvLinkReceiver : TvLinkReceiver {

    private val receiverState = MutableStateFlow(ReceiverState())
    override val state: StateFlow<ReceiverState> = receiverState.asStateFlow()

    private val incoming = MutableSharedFlow<RemoteCommand>()
    override val commands: Flow<RemoteCommand> = incoming.asSharedFlow()

    private val current = MutableStateFlow<PlaybackStatus?>(null)
    override val playback: StateFlow<PlaybackStatus?> = current.asStateFlow()

    override fun publish(status: PlaybackStatus?) {
        current.value = status
    }

    override suspend fun serve() {
        receiverState.value = ReceiverState(listening = true, address = ADDRESS)
        try {
            awaitCancellation()
        } finally {
            receiverState.value = ReceiverState()
        }
    }

    /** Команда от подключённого телефона. */
    suspend fun receive(command: RemoteCommand) = incoming.emit(command)

    fun connectPhone(name: String) {
        receiverState.update { it.copy(phones = it.phones + name) }
    }

    private companion object {
        const val ADDRESS = "192.168.1.50"
    }
}

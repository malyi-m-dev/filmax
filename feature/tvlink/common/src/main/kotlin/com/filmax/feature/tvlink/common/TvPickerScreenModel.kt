package com.filmax.feature.tvlink.common

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.filmax.core.domain.tvlink.RemoteCommand
import com.filmax.core.domain.tvlink.RemoteSession
import com.filmax.core.domain.tvlink.TvDevice
import com.filmax.core.domain.tvlink.TvDiscovery
import com.filmax.core.domain.tvlink.TvRemote
import com.filmax.core.presentation.BaseScreenModel
import com.filmax.feature.tvlink.common.navigation.TvPickerRoute
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine

/**
 * Выбор телевизора на телефоне: свои ТВ в сети (или адрес вручную), подключение и передача тайтла.
 * Поиск идёт, пока окно открыто, — модель живёт столько же, сколько окно. Запомненный ТВ выбирать не
 * нужно: окно сразу подключается к нему и отдаёт тайтл. Выбрали ТВ — окно уступает место пульту
 * ([TvPickerSideEffect.OpenRemote]).
 */
class TvPickerScreenModel(
    savedStateHandle: SavedStateHandle,
    private val discovery: TvDiscovery,
    private val remote: TvRemote,
) : BaseScreenModel<TvPickerState, TvPickerSideEffect, TvPickerEvent>(TvPickerState()) {

    /** Что запустить на выбранном ТВ; null — окно открыли ради пульта. */
    private val play: RemoteCommand.Play? = playOf(savedStateHandle.toRoute<TvPickerRoute>())

    init {
        onFetchData()
    }

    override fun onFetchData() {
        val withTitle = play != null
        screenModelScope { _ -> updateState { it.copy(withTitle = withTitle, title = pickerTitle(withTitle)) } }
        screenModelScope { _ ->
            combine(discovery.devices(), remote.paired) { found, paired -> withPairedFirst(found, paired) }
                .collect { devices -> updateState { it.copy(devices = devices) } }
        }
        screenModelScope { _ ->
            remote.session.collect { session ->
                val connectedTo = (session as? RemoteSession.Connected)?.device?.id
                updateState { it.copy(connectedTo = connectedTo) }
            }
        }
        screenModelScope { _ ->
            delay(SLOW_SEARCH_MS)
            updateState { it.copy(searchTakesLong = true) }
        }
        connectToPaired()
    }

    /**
     * Телефон и ТВ уже знакомы — «На ТВ» просто запускает тайтл, без выбора. ТВ, с которым связь
     * только что не вышла, не дёргаем: окно покажет список — выбрать его снова или другой.
     */
    private fun connectToPaired() {
        val tv = remote.paired.value ?: return
        if (remote.session.value is RemoteSession.Lost) return
        connect(tv)
    }

    override fun onEvent(event: TvPickerEvent) {
        when (event) {
            is TvPickerEvent.Choose -> onChoose(event.deviceId)
            TvPickerEvent.OpenManual -> onOpenManual()
            is TvPickerEvent.EditAddress -> onEditAddress(event.text)
            TvPickerEvent.ConnectByAddress -> onConnectByAddress()
            TvPickerEvent.Dismiss -> sendSideEffect(TvPickerSideEffect.Close)
        }
    }

    private fun onChoose(deviceId: String) {
        val device = state.devices.firstOrNull { it.id == deviceId } ?: return
        connect(device)
    }

    private fun onOpenManual() {
        screenModelScope { _ -> updateState { it.copy(manualOpen = true) } }
    }

    private fun onEditAddress(text: String) {
        screenModelScope { _ -> updateState { it.copy(address = text, addressInvalid = false) } }
    }

    private fun onConnectByAddress() {
        val device = discovery.deviceAt(state.address)
        if (device == null) {
            screenModelScope { _ -> updateState { it.copy(addressInvalid = true) } }
            return
        }
        connect(device)
    }

    /** Подключиться (связь с этим ТВ, если она есть, пульт не рвёт), отдать тайтл и открыть пульт. */
    private fun connect(device: TvDevice) {
        if (state.connectingTo != null) return
        screenModelScope { _ ->
            updateState { it.copy(connectingTo = device.id, failure = null) }
            val linked = remote.connect(device)
            if (!linked) {
                val reason = (remote.session.value as? RemoteSession.Lost)?.failure
                val failure = reason?.let { TvPickerFailure(device.name, it) }
                updateState { it.copy(connectingTo = null, failure = failure) }
                return@screenModelScope
            }
            play?.let(remote::send)
            postSideEffect(TvPickerSideEffect.OpenRemote)
        }
    }

    private companion object {
        /** Свой ТВ в домашней сети находится за секунду-две; дольше — его, скорее всего, нет. */
        const val SLOW_SEARCH_MS = 4_000L
    }
}

private fun playOf(route: TvPickerRoute): RemoteCommand.Play? {
    if (route.itemId <= 0) return null
    return RemoteCommand.Play(
        itemId = route.itemId,
        videoId = route.videoId,
        season = route.season,
        startSeconds = route.startSeconds,
    )
}

/**
 * Найденные ТВ и запомненный — первым, даже если поиск его не видит (роутер режет mDNS или ТВ
 * подключали по адресу). Если поиск его видит, берём найденного: адрес там свежее.
 */
private fun withPairedFirst(found: List<TvDevice>, paired: TvDevice?): List<TvDevice> {
    paired ?: return found
    val (own, others) = found.partition { it.id == paired.id }
    return listOf(own.firstOrNull() ?: paired) + others
}

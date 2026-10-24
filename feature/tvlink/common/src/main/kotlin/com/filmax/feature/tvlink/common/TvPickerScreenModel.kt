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

/**
 * Выбор телевизора на телефоне: свои ТВ в сети (или адрес вручную), подключение и передача тайтла.
 * Поиск идёт, пока окно открыто, — модель живёт столько же, сколько окно.
 */
class TvPickerScreenModel(
    savedStateHandle: SavedStateHandle,
    private val discovery: TvDiscovery,
    private val remote: TvRemote,
) : BaseScreenModel<TvPickerState, TvPickerSideEffect, TvPickerEvent>(TvPickerState()) {

    /** Что запустить на выбранном ТВ; null — окно открыли ради пульта. */
    private val play: RemoteCommand.Play? = savedStateHandle.toRoute<TvPickerRoute>().toPlay()

    init {
        onFetchData()
    }

    override fun onFetchData() {
        screenModelScope { _ -> updateState { it.copy(withTitle = play != null) } }
        screenModelScope { _ -> discovery.devices().collect { found -> updateState { it.copy(devices = found) } } }
        screenModelScope { _ ->
            remote.session.collect { session ->
                updateState { it.copy(connectedTo = (session as? RemoteSession.Connected)?.device?.id) }
            }
        }
        screenModelScope { _ ->
            delay(SLOW_SEARCH_MS)
            updateState { it.copy(searchTakesLong = true) }
        }
    }

    override fun onEvent(event: TvPickerEvent) {
        when (event) {
            is TvPickerEvent.Choose -> state.devices.firstOrNull { it.id == event.deviceId }?.let(::connect)
            TvPickerEvent.OpenManual -> screenModelScope { _ -> updateState { it.copy(manualOpen = true) } }
            is TvPickerEvent.EditAddress -> screenModelScope { _ ->
                updateState { it.copy(address = event.text, addressInvalid = false) }
            }
            TvPickerEvent.ConnectByAddress -> connectByAddress()
            TvPickerEvent.Dismiss -> screenModelScope { _ -> postSideEffect(TvPickerSideEffect.Close) }
        }
    }

    private fun connectByAddress() {
        val device = discovery.deviceAt(state.address)
        if (device == null) {
            screenModelScope { _ -> updateState { it.copy(addressInvalid = true) } }
            return
        }
        connect(device)
    }

    /** Подключиться (если связи с этим ТВ ещё нет), отдать тайтл и открыть пульт. */
    private fun connect(device: TvDevice) {
        if (state.connectingTo != null) return
        screenModelScope { _ ->
            updateState { it.copy(connectingTo = device.id, failure = null) }
            val linked = device.id == state.connectedTo || remote.connect(device)
            if (!linked) {
                val reason = (remote.session.value as? RemoteSession.Lost)?.failure
                updateState { picker ->
                    picker.copy(connectingTo = null, failure = reason?.let { TvPickerFailure(device.name, it) })
                }
                return@screenModelScope
            }
            play?.let(remote::send)
            postSideEffect(TvPickerSideEffect.OpenRemote)
        }
    }

    private companion object {
        /** Свой ТВ в домашней сети находится за секунду-две; дольше — его, скорее всего, нет. */
        const val SLOW_SEARCH_MS = 4_000L

        fun TvPickerRoute.toPlay(): RemoteCommand.Play? = takeIf { itemId > 0 }?.let {
            RemoteCommand.Play(itemId = itemId, videoId = videoId, season = season, startSeconds = startSeconds)
        }
    }
}

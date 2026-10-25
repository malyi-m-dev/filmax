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
 * нужно: окно сразу подключается к нему и отдаёт тайтл.
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
        screenModelScope { _ ->
            combine(discovery.devices(), remote.paired) { found, paired -> found.withPaired(paired) }
                .collect { devices -> updateState { it.copy(devices = devices) } }
        }
        screenModelScope { _ ->
            remote.session.collect { session ->
                updateState { it.copy(connectedTo = (session as? RemoteSession.Connected)?.device?.id) }
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

    /** Подключиться (связь с этим ТВ, если она есть, пульт не рвёт), отдать тайтл и открыть пульт. */
    private fun connect(device: TvDevice) {
        if (state.connectingTo != null) return
        screenModelScope { _ ->
            updateState { it.copy(connectingTo = device.id, failure = null) }
            val linked = remote.connect(device)
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

        /**
         * Найденные ТВ и запомненный — первым, даже если поиск его не видит (роутер режет mDNS или ТВ
         * подключали по адресу). Если поиск его видит, берём найденного: адрес там свежее.
         */
        fun List<TvDevice>.withPaired(paired: TvDevice?): List<TvDevice> {
            paired ?: return this
            val (own, others) = partition { it.id == paired.id }
            return listOf(own.firstOrNull() ?: paired) + others
        }
    }
}

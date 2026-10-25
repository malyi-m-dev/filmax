package com.filmax.data.tvlink

import com.filmax.core.domain.tvlink.TvDevice
import com.filmax.data.tvlink.security.LinkCrypto
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Что связь помнит между запусками: на телефоне — телевизор, к которому подключались, на ТВ — свой
 * постоянный id. Без этого телефон после перезапуска забывал ТВ, а узнать «тот же» ТВ по имени или
 * адресу нельзя: имя mDNS меняет при совпадении с соседом, адрес — роутер.
 */
internal class TvLinkMemory(
    private val settings: Settings,
    private val crypto: LinkCrypto,
) {

    private val pairedTv = MutableStateFlow(loadPaired())
    val paired: StateFlow<TvDevice?> = pairedTv.asStateFlow()

    fun remember(device: TvDevice) {
        if (pairedTv.value == device) return
        settings.putString(KEY_ID, device.id)
        settings.putString(KEY_NAME, device.name)
        settings.putString(KEY_HOST, device.host)
        settings.putInt(KEY_PORT, device.port)
        pairedTv.value = device
    }

    fun forget() {
        listOf(KEY_ID, KEY_NAME, KEY_HOST, KEY_PORT).forEach(settings::remove)
        pairedTv.value = null
    }

    /** Постоянный id этого ТВ: создаётся при первом запуске приёмника и больше не меняется. */
    @OptIn(ExperimentalStdlibApi::class)
    fun ownId(): String = settings.getStringOrNull(KEY_OWN_ID)
        ?: crypto.randomBytes(OWN_ID_BYTES).toHexString().also { settings.putString(KEY_OWN_ID, it) }

    private fun loadPaired(): TvDevice? {
        val id = settings.getStringOrNull(KEY_ID)
        val host = settings.getStringOrNull(KEY_HOST)
        val port = settings.getIntOrNull(KEY_PORT)
        if (id == null || host == null || port == null) return null
        return TvDevice(id = id, name = settings.getString(KEY_NAME, host), host = host, port = port)
    }

    private companion object {
        const val KEY_ID = "tvlink_paired_id"
        const val KEY_NAME = "tvlink_paired_name"
        const val KEY_HOST = "tvlink_paired_host"
        const val KEY_PORT = "tvlink_paired_port"
        const val KEY_OWN_ID = "tvlink_own_id"

        /** 64 бита: двух одинаковых id у телевизоров одного дома не случится. */
        const val OWN_ID_BYTES = 8
    }
}

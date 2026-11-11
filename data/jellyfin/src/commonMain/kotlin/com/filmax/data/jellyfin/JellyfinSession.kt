package com.filmax.data.jellyfin

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

/** Вход на сервер: куда ходить, под кем и с каким токеном. */
@Serializable
internal data class StoredSession(
    val address: String,
    val serverName: String,
    val userId: String,
    val userName: String,
    val token: String,
)

/**
 * Сессия домашнего сервера — в настройках устройства, рядом с токенами kino.pub. Обе живут
 * независимо: переключение источника не разлогинивает ни один из них.
 */
internal class JellyfinSession(private val settings: Settings) {

    private val state = MutableStateFlow(load())

    val current: StoredSession? get() = state.value

    val signedIn: Flow<Boolean> = state.map { it != null }

    /**
     * Постоянный id устройства для заголовка авторизации. Сервер по нему узнаёт устройство
     * между входами: с новым id каждый запуск плодил бы в панели сервера «новые» устройства.
     */
    val deviceId: String by lazy {
        settings.getStringOrNull(DEVICE_KEY) ?: newDeviceId().also { settings.putString(DEVICE_KEY, it) }
    }

    /** Текущая сессия; без входа — ошибка с 401, её экраны покажут как «сессия истекла». */
    fun require(): StoredSession = current ?: throw IllegalStateException(NOT_SIGNED_IN)

    fun save(session: StoredSession) {
        settings.putString(SESSION_KEY, json.encodeToString(StoredSession.serializer(), session))
        state.value = session
    }

    fun clear() {
        settings.remove(SESSION_KEY)
        state.value = null
    }

    private fun load(): StoredSession? = settings.getStringOrNull(SESSION_KEY)
        ?.let { stored -> runCatching { json.decodeFromString(StoredSession.serializer(), stored) }.getOrNull() }

    private fun newDeviceId(): String = Random.nextBytes(DEVICE_ID_BYTES).joinToString("") { byte ->
        (byte.toInt() and BYTE_MASK).toString(HEX).padStart(2, '0')
    }

    private companion object {
        const val SESSION_KEY = "jellyfin_session"
        const val DEVICE_KEY = "jellyfin_device_id"
        const val NOT_SIGNED_IN = "Нет входа на домашний сервер (401)"
        const val DEVICE_ID_BYTES = 16
        const val BYTE_MASK = 0xFF
        const val HEX = 16
        val json = Json { ignoreUnknownKeys = true }
    }
}

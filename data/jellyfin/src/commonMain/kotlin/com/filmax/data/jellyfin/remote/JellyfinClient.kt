package com.filmax.data.jellyfin.remote

import com.filmax.data.jellyfin.JellyfinSession
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Кто ходит на сервер: подпись в заголовке авторизации и в панели устройств сервера. */
internal data class JellyfinClientInfo(
    val device: String,
    val version: String,
)

internal val jellyfinJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    // Тела запросов: значения по умолчанию сервер ждёт явно, а null — пропущенным полем.
    encodeDefaults = true
    explicitNulls = false
}

/**
 * Клиент домашнего сервера — отдельный от kino.pub: другой адрес (он у каждого свой), другая
 * авторизация (заголовок `MediaBrowser … Token=`, без OAuth и обновления токенов). Движок —
 * общий: на Android это тот же OkHttp с инспектором запросов в debug.
 */
internal fun jellyfinHttpClient(
    engine: HttpClientEngine,
    session: JellyfinSession,
    client: JellyfinClientInfo,
): HttpClient = HttpClient(engine) {
    expectSuccess = true
    install(ContentNegotiation) { json(jellyfinJson) }
    install(HttpTimeout) {
        // Сервер в своей сети отвечает быстро; дольше ждать — значит, он выключен или адрес чужой.
        connectTimeoutMillis = CONNECT_TIMEOUT_MS
        requestTimeoutMillis = REQUEST_TIMEOUT_MS
    }
    defaultRequest {
        // Блок выполняется на каждый запрос: после входа заголовок сразу несёт токен.
        header(HttpHeaders.Authorization, authorization(client, session))
    }
}

/**
 * Заголовок авторизации Jellyfin. Клиент, устройство и его id сервер требует и до входа — без
 * них `AuthenticateByName` отвечает отказом. Кавычки и запятые из имени устройства вычищаются:
 * они ломают разбор заголовка.
 */
internal fun authorization(client: JellyfinClientInfo, session: JellyfinSession): String = buildString {
    append("MediaBrowser Client=\"$CLIENT_NAME\"")
    append(", Device=\"${client.device.headerSafe()}\"")
    append(", DeviceId=\"${session.deviceId}\"")
    append(", Version=\"${client.version.headerSafe()}\"")
    session.current?.token?.let { append(", Token=\"$it\"") }
}

private fun String.headerSafe(): String = filterNot { it == '"' || it == ',' || it == '=' }.ifBlank { "Android" }

private const val CLIENT_NAME = "Filmax"
private const val CONNECT_TIMEOUT_MS = 5_000L
private const val REQUEST_TIMEOUT_MS = 30_000L

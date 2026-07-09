package com.filmax.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

const val BASE_URL = "https://smarttvcdn.online/"

val networkJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

/**
 * Общий Ktor [HttpClient]. [engine] предоставляется платформой
 * (OkHttp на Android — с Chucker-перехватчиком, Darwin на iOS).
 */
// Намеренно ловим Throwable в refreshTokens и «глотаем» его: любой транзиентный сбой обмена не
// должен ронять клиент/сессию (как и граница ошибок в safeRequest). CancellationException — выше.
@Suppress("TooGenericExceptionCaught", "SwallowedException")
fun buildHttpClient(
    engine: HttpClientEngine,
    tokenStorage: TokenStorage,
    baseUrl: String = BASE_URL,
    enableLogging: Boolean = false,
): HttpClient = HttpClient(engine) {
    expectSuccess = true

    install(ContentNegotiation) {
        json(networkJson)
    }

    install(Auth) {
        bearer {
            loadTokens {
                tokenStorage.getAccessToken()?.let { token ->
                    BearerTokens(token, tokenStorage.getRefreshToken().orEmpty())
                }
            }
            // Access протух → Ktor вызывает refreshTokens. Меняем refresh_token на новую пару
            // токенов через OAuth-эндпоинт и повторяем исходный запрос с новым access — без
            // форс-релогина. Запрос обновления идёт через переданный [client] (у него отключён
            // повторный Auth), поэтому рекурсии на сам refresh-эндпоинт нет.
            //
            // Особый случай — свежий вход device-flow: кэш loadTokens на старте был пуст,
            // первый запрос ушёл без заголовка и получил 401; тогда refresh_token в хранилище
            // ещё «старый», обмен даст актуальные токены (или, если его нет, — logout).
            refreshTokens {
                // Если в хранилище уже более свежий access, чем протухший (свежий device-логин —
                // кэш loadTokens на старте был пуст; либо параллельный запрос уже обновил токены) —
                // используем его без сетевого обмена (не тратим refresh_token зря).
                val storedAccess = tokenStorage.getAccessToken()
                if (!storedAccess.isNullOrBlank() && storedAccess != oldTokens?.accessToken) {
                    return@refreshTokens BearerTokens(storedAccess, tokenStorage.getRefreshToken().orEmpty())
                }
                val refresh = tokenStorage.getRefreshToken()
                if (refresh.isNullOrBlank()) {
                    // Нечем обновляться — единый сценарий logout, без цикла 401.
                    tokenStorage.clear()
                    return@refreshTokens null
                }
                try {
                    val response: OAuthTokenResponse = client.post(OAUTH_DEVICE_PATH) {
                        parameter("grant_type", "refresh_token")
                        parameter("client_id", OAUTH_CLIENT_ID)
                        parameter("client_secret", OAUTH_CLIENT_SECRET)
                        parameter("refresh_token", refresh)
                    }.body()
                    tokenStorage.save(response.accessToken, response.refreshToken)
                    BearerTokens(response.accessToken, response.refreshToken)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (rejected: ClientRequestException) {
                    // 4xx от OAuth (invalid_grant): refresh_token действительно невалиден → logout.
                    tokenStorage.clear()
                    null
                } catch (transient: Throwable) {
                    // Транзиентный сбой (offline/timeout/5xx): НЕ разлогиниваем — обновление не удалось,
                    // исходный запрос вернёт 401, но сессия сохранится до восстановления сети.
                    null
                }
            }
            sendWithoutRequest { true }
        }
    }

    if (enableLogging) {
        install(Logging) {
            level = LogLevel.BODY
        }
    }

    defaultRequest {
        url(baseUrl)
    }
}

/**
 * Ответ OAuth-эндпоинта при обмене refresh_token (те же поля, что и `TokenDto` в `:data:auth`;
 * дублируется локально, чтобы сетевой слой не зависел от `:data:auth`).
 */
@Serializable
private data class OAuthTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Int = 0,
)

package com.filmax.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

const val BASE_URL = "https://smarttvcdn.online/"

val networkJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

/**
 * Общий Ktor [HttpClient]. [engine] предоставляется платформой
 * (OkHttp на Android — с Chucker-перехватчиком, Darwin на iOS).
 */
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
                tokenStorage.getAccessToken()?.let { BearerTokens(it, "") }
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

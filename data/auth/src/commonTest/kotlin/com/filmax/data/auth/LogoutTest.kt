package com.filmax.data.auth

import com.filmax.core.network.TokenStorage
import com.filmax.core.network.buildHttpClient
import com.filmax.data.auth.remote.AuthApi
import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Выход отвязывает устройство от аккаунта kino.pub — иначе каждый повторный вход оставлял бы в
 * списке устройств ещё одно мёртвое. Сбой отвязки выходу не мешает.
 */
class LogoutTest {

    private val tokens = TokenStorage(MapSettings())
    private val paths = mutableListOf<String>()

    @Test
    fun `logout unlinks the device and signs out`() = runTest {
        val kinoPub = MockEngine { request ->
            paths += request.url.encodedPath
            respond("""{"status":200}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val repository = repository(kinoPub)

        repository.logout()

        assertEquals(listOf("/api/v1/device/unlink"), paths)
        assertFalse(repository.isAuthenticated.first())
    }

    @Test
    fun `failed unlink still signs out`() = runTest {
        val repository = repository(MockEngine { respondError(HttpStatusCode.ServiceUnavailable) })

        repository.logout()

        assertFalse(repository.isAuthenticated.first())
    }

    private suspend fun repository(engine: MockEngine): AuthRepositoryImpl {
        tokens.save("access", "refresh")
        return AuthRepositoryImpl(AuthApi(buildHttpClient(engine, tokens)), tokens)
    }
}

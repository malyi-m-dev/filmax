package com.filmax.data.user

import com.filmax.core.domain.common.RequestResult
import com.filmax.core.network.TokenStorage
import com.filmax.core.network.buildHttpClient
import com.filmax.data.user.remote.UserApi
import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * `device/notify` сразу после входа: устройство в аккаунте kino.pub получает имя, железо и версию.
 * Без этого в списке устройств аккаунта оно остаётся «unknown / unknown / unknown».
 */
class DeviceRegistrationTest {

    private val requests = mutableListOf<HttpRequestData>()
    private val tokens = TokenStorage(MapSettings())

    @Test
    fun `device is described right after the device-flow login`() = runTest {
        val repository = UserRepositoryImpl(UserApi(buildHttpClient(kinoPub(), tokens)))
        // До входа клиент уже ходил в сеть (код устройства, опрос) — без токенов.
        assertIs<RequestResult.Error>(repository.getProfile())

        tokens.save(ACCESS, REFRESH)
        val result = repository.registerDevice("Xiaomi MiTV-MZTU1", "mzt", "Filmax · Android 14")

        assertIs<RequestResult.Success<Unit>>(result)
        val notify = requests.last { it.url.encodedPath.endsWith("device/notify") }
        assertEquals("Bearer $ACCESS", notify.headers[HttpHeaders.Authorization])
    }

    /** kino.pub: без токена — 401 с вызовом Bearer, с токеном — `{"status":200}`. */
    private fun kinoPub() = MockEngine { request ->
        requests += request
        if (request.headers[HttpHeaders.Authorization] == "Bearer $ACCESS") {
            respond("""{"status":200}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        } else {
            respond(
                """{"status":401,"error":"unauthorized"}""",
                HttpStatusCode.Unauthorized,
                headersOf(
                    HttpHeaders.ContentType to listOf("application/json"),
                    HttpHeaders.WWWAuthenticate to listOf("""Bearer realm="api""""),
                ),
            )
        }
    }

    private companion object {
        const val ACCESS = "fresh-access"
        const val REFRESH = "fresh-refresh"
    }
}

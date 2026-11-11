package com.filmax.core.network.kinopub

import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Обновление токенов kino.pub. Ошибка здесь разлогинивает всех сразу, поэтому три исхода обмена
 * refresh_token проверяются отдельно: новый токен, отказ сервера и сбой сети.
 */
class TokenRefreshTest {

    private val storage = TokenStorage(MapSettings())

    /** Что отвечает OAuth-эндпоинт на обмен refresh_token. */
    private var oauth: MockRequestHandleScope.() -> HttpResponseData = { json(FRESH_TOKENS) }
    private val oauthCalls = mutableListOf<String?>()

    private val engine = MockEngine { request -> answer(request) }

    @Test
    fun `expired access is refreshed and the request is retried with the new token`() = runTest {
        storage.save("old-access", "old-refresh")

        val response = client().get(USER_PATH)

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("new-access", storage.getAccessToken())
        assertEquals("new-refresh", storage.getRefreshToken())
        assertEquals(listOf<String?>("old-refresh"), oauthCalls.toList())
    }

    @Test
    fun `refresh token rejected by the server signs out`() = runTest {
        storage.save("old-access", "old-refresh")
        oauth = { respond("""{"error":"invalid_grant"}""", HttpStatusCode.BadRequest, JSON_HEADERS) }

        assertFailsWith<ClientRequestException> { client().get(USER_PATH) }

        assertNull(storage.getAccessToken())
        assertNull(storage.getRefreshToken())
    }

    @Test
    fun `network failure during refresh keeps the session`() = runTest {
        storage.save("old-access", "old-refresh")
        oauth = { error("нет сети") }

        assertFailsWith<ClientRequestException> { client().get(USER_PATH) }

        assertEquals("old-access", storage.getAccessToken())
        assertEquals("old-refresh", storage.getRefreshToken())
    }

    @Test
    fun `token saved by a fresh sign-in is used without spending the refresh token`() = runTest {
        val client = client()
        storage.save("new-access", "new-refresh")

        val response = client.get(USER_PATH)

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(oauthCalls.isEmpty())
    }

    private fun client() = buildKinopubHttpClient(engine, storage, baseUrl = "https://api.test/")

    /** API пускает только с новым access, OAuth-эндпоинт отвечает по [oauth]. */
    private fun MockRequestHandleScope.answer(request: HttpRequestData): HttpResponseData {
        if (request.url.encodedPath.endsWith(OAUTH_DEVICE_PATH)) {
            oauthCalls += request.url.parameters["refresh_token"]
            return oauth()
        }
        val authorized = request.headers[HttpHeaders.Authorization] == "Bearer new-access"
        return if (authorized) json("{}") else respond("", HttpStatusCode.Unauthorized, BEARER_CHALLENGE)
    }

    private fun MockRequestHandleScope.json(body: String) = respond(body, HttpStatusCode.OK, JSON_HEADERS)

    private companion object {
        const val USER_PATH = "api/v1/user"
        const val FRESH_TOKENS = """{"access_token":"new-access","refresh_token":"new-refresh","expires_in":3600}"""
        val JSON_HEADERS = headersOf(HttpHeaders.ContentType, "application/json")

        /** Без схемы Bearer в WWW-Authenticate Ktor не узнаёт, что это его провайдер. */
        val BEARER_CHALLENGE = headersOf(HttpHeaders.WWWAuthenticate, "Bearer")
    }
}

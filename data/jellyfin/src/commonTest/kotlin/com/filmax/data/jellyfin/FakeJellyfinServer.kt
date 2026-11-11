package com.filmax.data.jellyfin

import com.filmax.data.jellyfin.remote.JellyfinApi
import com.filmax.data.jellyfin.remote.JellyfinClientInfo
import com.filmax.data.jellyfin.remote.jellyfinHttpClient
import com.russhwolf.settings.MapSettings
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf

/** Запрос, дошедший до «сервера»: метод, путь, параметры, заголовок авторизации и тело. */
internal data class SeenRequest(
    val method: HttpMethod,
    val path: String,
    val parameters: Map<String, String>,
    val authorization: String?,
    val body: String,
)

/**
 * Jellyfin на подменённом движке Ktor: ответ — по методу и пути, всё, что пришло, — в [requests].
 * Неописанный путь отвечает 404, как и настоящий сервер.
 */
internal class FakeJellyfinServer(private val routes: Map<String, String> = emptyMap()) {

    val requests = mutableListOf<SeenRequest>()
    val settings = MapSettings()
    val session = JellyfinSession(settings)
    val ids = JellyfinIds(settings)

    private val engine = MockEngine { request -> answer(request) }

    val api = JellyfinApi(
        client = jellyfinHttpClient(engine, session, JellyfinClientInfo(device = "Test TV", version = "2.8.0")),
        session = session,
    )

    val context = JellyfinContext(api, ids, session)

    fun signIn() = session.save(
        StoredSession(address = ADDRESS, serverName = "Дом", userId = USER_ID, userName = "anna", token = TOKEN),
    )

    fun requestsTo(path: String): List<SeenRequest> = requests.filter { it.path == path }

    private fun MockRequestHandleScope.answer(request: HttpRequestData): HttpResponseData {
        val path = request.url.encodedPath
        requests += SeenRequest(
            method = request.method,
            path = path,
            parameters = request.url.parameters.names().associateWith { request.url.parameters[it].orEmpty() },
            authorization = request.headers[HttpHeaders.Authorization],
            body = (request.body as? OutgoingContent.ByteArrayContent)?.bytes()?.decodeToString().orEmpty(),
        )
        val json = routes["${request.method.value} $path"] ?: routes[path]
            ?: return respond("", HttpStatusCode.NotFound)
        return respond(json, headers = headersOf(HttpHeaders.ContentType, "application/json"))
    }

    companion object {
        const val ADDRESS = "http://192.168.1.20:8096"
        const val USER_ID = "u1"
        const val TOKEN = "secret-token"
    }
}

package com.filmax.data.jellyfin.remote

import com.filmax.data.jellyfin.JellyfinSession
import com.filmax.data.jellyfin.remote.dto.AuthenticateByNameBody
import com.filmax.data.jellyfin.remote.dto.AuthenticationResultDto
import com.filmax.data.jellyfin.remote.dto.ItemDto
import com.filmax.data.jellyfin.remote.dto.ItemsResponseDto
import com.filmax.data.jellyfin.remote.dto.PlaybackInfoBody
import com.filmax.data.jellyfin.remote.dto.PlaybackInfoResponseDto
import com.filmax.data.jellyfin.remote.dto.PlaybackProgressBody
import com.filmax.data.jellyfin.remote.dto.PublicSystemInfoDto
import com.filmax.data.jellyfin.remote.dto.QuickConnectBody
import com.filmax.data.jellyfin.remote.dto.QuickConnectResultDto
import com.filmax.data.jellyfin.remote.dto.UserDataBody
import com.filmax.data.jellyfin.remote.dto.UserDataDto
import com.filmax.data.jellyfin.remote.dto.UserDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * REST API Jellyfin (пути — по OpenAPI сервера 10.9+). До входа адрес сервера передаётся явно,
 * после — берётся из сессии вместе с id пользователя.
 */
// Тонкая обёртка над эндпоинтами: по функции на запрос, логики нет.
@Suppress("TooManyFunctions")
internal class JellyfinApi(
    private val client: HttpClient,
    private val session: JellyfinSession,
) {

    // ── До входа ─────────────────────────────────────────────────────────────

    suspend fun publicInfo(address: String): PublicSystemInfoDto =
        client.get("$address/System/Info/Public").body()

    suspend fun quickConnectEnabled(address: String): Boolean =
        client.get("$address/QuickConnect/Enabled").body()

    suspend fun authenticateByName(address: String, username: String, password: String): AuthenticationResultDto =
        client.post("$address/Users/AuthenticateByName") { jsonBody(AuthenticateByNameBody(username, password)) }.body()

    suspend fun initiateQuickConnect(address: String): QuickConnectResultDto =
        client.post("$address/QuickConnect/Initiate").body()

    suspend fun quickConnectState(address: String, secret: String): QuickConnectResultDto =
        client.get("$address/QuickConnect/Connect") { parameter("secret", secret) }.body()

    suspend fun authenticateWithQuickConnect(address: String, secret: String): AuthenticationResultDto =
        client.post("$address/Users/AuthenticateWithQuickConnect") { jsonBody(QuickConnectBody(secret)) }.body()

    // ── Аккаунт ──────────────────────────────────────────────────────────────

    suspend fun me(): UserDto = client.get(url("Users/Me")).body()

    suspend fun logout() {
        client.post(url("Sessions/Logout"))
    }

    // ── Каталог ──────────────────────────────────────────────────────────────

    suspend fun items(query: ItemsQuery): ItemsResponseDto =
        client.get(url("Items")) {
            withUser()
            itemsParameters(query)
        }.body()

    suspend fun item(guid: String): ItemDto = client.get(url("Items/$guid")) { withUser() }.body()

    /** Серии сериала по порядку; виртуальные «недостающие» серии сервер не отдаёт. */
    suspend fun episodes(seriesGuid: String): ItemsResponseDto =
        client.get(url("Shows/$seriesGuid/Episodes")) {
            withUser()
            parameter("isMissing", false)
            parameter("fields", "Overview")
        }.body()

    suspend fun similar(guid: String, limit: Int): ItemsResponseDto =
        client.get(url("Items/$guid/Similar")) {
            withUser()
            parameter("limit", limit)
            parameter("fields", LIST_FIELDS)
        }.body()

    suspend fun genres(itemTypes: List<String>): ItemsResponseDto =
        client.get(url("Genres")) {
            withUser()
            parameter("includeItemTypes", itemTypes.joinToString(","))
            parameter("sortBy", "SortName")
        }.body()

    // ── Просмотр ─────────────────────────────────────────────────────────────

    suspend fun playbackInfo(guid: String, body: PlaybackInfoBody): PlaybackInfoResponseDto =
        client.post(url("Items/$guid/PlaybackInfo")) {
            withUser()
            jsonBody(body)
        }.body()

    suspend fun reportPlaybackStart(body: PlaybackProgressBody) {
        client.post(url("Sessions/Playing")) { jsonBody(body) }
    }

    suspend fun reportPlaybackProgress(body: PlaybackProgressBody) {
        client.post(url("Sessions/Playing/Progress")) { jsonBody(body) }
    }

    suspend fun userData(guid: String): UserDataDto = client.get(url("UserItems/$guid/UserData")) { withUser() }.body()

    suspend fun updateUserData(guid: String, body: UserDataBody) {
        client.post(url("UserItems/$guid/UserData")) {
            withUser()
            jsonBody(body)
        }
    }

    suspend fun setPlayed(guid: String, played: Boolean) {
        val path = url("UserPlayedItems/$guid")
        if (played) client.post(path) { withUser() } else client.delete(path) { withUser() }
    }

    suspend fun setFavorite(guid: String, favorite: Boolean) {
        val path = url("UserFavoriteItems/$guid")
        if (favorite) client.post(path) { withUser() } else client.delete(path) { withUser() }
    }

    private fun url(path: String): String = "${session.require().address}/$path"

    private fun HttpRequestBuilder.withUser() {
        parameter("userId", session.require().userId)
    }

    companion object {
        /** Поля карточки в выдаче: описание для hero, жанры и страна — для строк меты. */
        const val LIST_FIELDS = "Overview,Genres,ProductionLocations,ProviderIds"
    }
}

/**
 * Параметры выдачи. Массивы сервер принимает через запятую, а имена жанров — через «|»: в имени
 * жанра бывает запятая.
 */
private fun HttpRequestBuilder.itemsParameters(query: ItemsQuery) {
    parameter("recursive", query.recursive)
    parameter("fields", JellyfinApi.LIST_FIELDS)
    parameter("enableUserData", true)
    parameter("startIndex", query.startIndex)
    query.limit?.let { parameter("limit", it) }
    listParameter("includeItemTypes", query.includeItemTypes)
    listParameter("sortBy", query.sortBy)
    if (query.sortBy.isNotEmpty()) parameter("sortOrder", if (query.ascending) "Ascending" else "Descending")
    listParameter("genreIds", query.genreIds)
    if (query.genres.isNotEmpty()) parameter("genres", query.genres.joinToString("|"))
    listParameter("filters", query.filters)
    listParameter("personTypes", query.personTypes)
    listParameter("seriesStatus", query.seriesStatus)
    listParameter("imageTypes", query.imageTypes)
    query.searchTerm?.let { parameter("searchTerm", it) }
    query.parentId?.let { parameter("parentId", it) }
    query.person?.let { parameter("person", it) }
    query.is4K?.let { parameter("is4K", it) }
    query.minCommunityRating?.let { parameter("minCommunityRating", it) }
    query.minPremiereDate?.let { parameter("minPremiereDate", it) }
    query.maxPremiereDate?.let { parameter("maxPremiereDate", it) }
}

/** JSON-тело: без явного типа содержимого ContentNegotiation тело не сериализует. */
private inline fun <reified T : Any> HttpRequestBuilder.jsonBody(body: T) {
    contentType(ContentType.Application.Json)
    setBody(body)
}

private fun HttpRequestBuilder.listParameter(name: String, values: List<String>) {
    if (values.isNotEmpty()) parameter(name, values.joinToString(","))
}

package com.filmax.data.tmdb.remote

import com.filmax.data.tmdb.remote.dto.TmdbCreditsDto
import com.filmax.data.tmdb.remote.dto.TmdbFindDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Клиент TMDB. Отдельный от kino.pub: другой хост и свой api_key. Bearer-авторизации нет —
 * поэтому НЕ переиспользуем общий Ktor-клиент (у него плагин Auth с токеном kino.pub), а строим
 * свой поверх того же движка. Язык `ru-RU`: имена персонажей и актёров придут по-русски, где TMDB
 * их локализовал, иначе — на оригинале.
 */
internal class TmdbApi(
    engine: HttpClientEngine,
    private val apiKey: String,
) {
    val hasKey: Boolean get() = apiKey.isNotBlank()

    private val client = HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        defaultRequest {
            url("https://api.themoviedb.org/3/")
        }
    }

    /** api_key и язык — в каждый запрос: внутри defaultRequest `parameter` недоступен (другой receiver). */
    private fun HttpRequestBuilder.commonParams() {
        parameter("api_key", apiKey)
        parameter("language", "ru-RU")
    }

    suspend fun findByImdb(imdbTag: String): TmdbFindDto =
        client.get("find/$imdbTag") {
            commonParams()
            parameter("external_source", "imdb_id")
        }.body()

    suspend fun movieCredits(id: Int): TmdbCreditsDto =
        client.get("movie/$id/credits") { commonParams() }.body()

    suspend fun tvCredits(id: Int): TmdbCreditsDto =
        client.get("tv/$id/credits") { commonParams() }.body()
}

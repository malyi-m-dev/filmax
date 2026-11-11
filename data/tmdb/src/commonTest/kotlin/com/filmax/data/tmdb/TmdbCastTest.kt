package com.filmax.data.tmdb

import com.filmax.data.tmdb.remote.TmdbApi
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Фото актёров из TMDB: тег IMDb, тип тайтла из ответа и тишина при любой неудаче. */
class TmdbCastTest {

    private val requests = mutableListOf<Url>()
    private var routes: Map<String, String> = mapOf(
        "/3/find/tt0123456" to """{"movie_results":[],"tv_results":[{"id":77}]}""",
        "/3/tv/77/credits" to """{"cast":[{"name":"Луис Хофман","character":"Йонас","profile_path":"/p.jpg"},
            {"name":"Лиза Викари","character":""}]}""",
    )

    private val engine = MockEngine { request ->
        requests += request.url
        val body = routes[request.url.encodedPath] ?: return@MockEngine respond("", HttpStatusCode.NotFound)
        respond(body, headers = headersOf(HttpHeaders.ContentType, "application/json"))
    }

    private fun repository(key: String = "key") = TmdbCastRepositoryImpl(TmdbApi(engine, key))

    @Test
    fun `numeric imdb id becomes a padded tt tag and a series takes tv credits`() = runTest {
        val cast = repository().getCast("123456")

        assertEquals(listOf("/3/find/tt0123456", "/3/tv/77/credits"), requests.map { it.encodedPath })
        assertEquals("Йонас", cast.first().character)
        assertEquals("https://image.tmdb.org/t/p/w185/p.jpg", cast.first().photoUrl)
        // Пустая роль — не роль: под фото ничего не пишем.
        assertNull(cast[1].character)
    }

    @Test
    fun `without a key or an imdb id nothing is requested`() = runTest {
        assertTrue(repository(key = "").getCast("123456").isEmpty())
        assertTrue(repository().getCast(null).isEmpty())
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `failure is an empty cast, not an error on the details screen`() = runTest {
        routes = emptyMap()

        assertTrue(repository().getCast("tt0123456").isEmpty())
    }
}

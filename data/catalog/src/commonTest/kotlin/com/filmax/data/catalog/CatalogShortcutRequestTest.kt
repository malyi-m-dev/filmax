package com.filmax.data.catalog

import com.filmax.core.domain.catalog.CatalogShortcut
import com.filmax.core.domain.catalog.model.ItemPage
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.network.networkJson
import com.filmax.data.catalog.remote.CatalogApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Готовые выборки kino.pub на подменённом движке: какой путь уходит на сервер. Фейковый data-слой
 * mock-сборки путей не знает — раньше «Новинки» звали несуществующий `items/new`, получали 404, и
 * ряд на Главной молча оставался пустым.
 */
class CatalogShortcutRequestTest {

    private val requests = mutableListOf<Url>()

    private val engine = MockEngine { request ->
        requests += request.url
        respond(TV_SHOW_PAGE, headers = headersOf(HttpHeaders.ContentType, "application/json"))
    }

    private val repository = CatalogRepositoryImpl(
        CatalogApi(HttpClient(engine) { install(ContentNegotiation) { json(networkJson) } }),
    )

    @Test
    fun `fresh titles come from items-fresh, not the missing items-new`() = runTest {
        repository.getShortcutItems(CatalogShortcut.FRESH, ItemType.SERIES, page = 2)

        val url = requests.single()
        assertEquals("/api/v1/items/fresh", url.encodedPath)
        assertEquals("serial", url.parameters["type"])
        assertEquals("2", url.parameters["page"])
    }

    @Test
    fun `each shortcut goes to its own path`() = runTest {
        CatalogShortcut.entries.forEach { shortcut -> repository.getShortcutItems(shortcut, ItemType.MOVIE) }

        assertEquals(
            listOf("/api/v1/items/hot", "/api/v1/items/popular", "/api/v1/items/fresh"),
            requests.map { it.encodedPath },
        )
    }

    @Test
    fun `tv show from a shortcut is parsed as an episodic title, not a movie`() = runTest {
        val result = repository.getShortcutItems(CatalogShortcut.POPULAR, ItemType.TV_SHOW)

        val item = assertIs<RequestResult.Success<ItemPage>>(result).data.items.single()
        assertEquals(ItemType.TV_SHOW, item.type)
        assertTrue(item.type.episodic)
        assertEquals("tvshow", requests.single().parameters["type"])
    }

    private companion object {
        /** Страница выборки в формате kino.pub: одно ТВ-шоу с сезоном. */
        const val TV_SHOW_PAGE = """
            {"status":200,"items":[{"id":77,"title":"Вечерний выпуск","type":"tvshow","year":2024,
            "seasons":[{"number":1,"episodes":[{"id":1,"number":1,"title":"Первый выпуск"}]}]}],
            "pagination":{"total":1,"current":1,"perpage":20}}
        """
    }
}

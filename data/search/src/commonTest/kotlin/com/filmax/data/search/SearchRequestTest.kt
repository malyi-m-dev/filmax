package com.filmax.data.search

import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.network.networkJson
import com.filmax.data.search.remote.SearchApi
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
import kotlin.test.assertNull

/** Поиск kino.pub на подменённом движке: какие параметры уходят и что приходит экрану. */
class SearchRequestTest {

    private val requests = mutableListOf<Url>()

    private val engine = MockEngine { request ->
        requests += request.url
        respond(DUPLICATED_PAGE, headers = headersOf(HttpHeaders.ContentType, "application/json"))
    }

    private val repository = SearchRepositoryImpl(
        SearchApi(HttpClient(engine) { install(ContentNegotiation) { json(networkJson) } }),
    )

    @Test
    fun `query goes to items-search with the type in kinopub terms`() = runTest {
        repository.search("дюна", ItemType.SERIES, perPage = 20)

        val url = requests.single()
        assertEquals("/api/v1/items/search", url.encodedPath)
        assertEquals("дюна", url.parameters["q"])
        assertEquals("serial", url.parameters["type"])
        assertEquals("20", url.parameters["perpage"])
    }

    @Test
    fun `search over every type sends no type at all`() = runTest {
        repository.search("дюна", type = null, perPage = 20)

        assertNull(requests.single().parameters["type"])
    }

    @Test
    fun `title found twice comes once, otherwise the list crashes on a repeated key`() = runTest {
        val items = repository.searchByActor("Тимоти Шаламе", perPage = 20).getOrNull().orEmpty()

        assertEquals(listOf(5), items.map { it.id })
        assertEquals("Тимоти Шаламе", requests.single().parameters["actor"])
    }

    private companion object {
        /** kino.pub отдал один тайтл дважды: актёр в двух ролях. */
        const val DUPLICATED_PAGE = """
            {"status":200,"items":[{"id":5,"title":"Дюна","type":"movie","year":2021},
            {"id":5,"title":"Дюна","type":"movie","year":2021}],
            "pagination":{"total":1,"current":1,"perpage":20}}
        """
    }
}

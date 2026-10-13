package com.filmax.feature.search.common

import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeCatalogRepository
import com.filmax.data.fake.FakeLatency
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Постраничная витрина по нескольким типам: склейка, конец выдачи, оценка размера, гонки чипов. */
class CatalogPagerTest {

    private val backend = FakeBackend(latency = FakeLatency.None)

    @Test
    fun `first page mixes types in turn`() = runTest {
        val pager = CatalogPager(FakeCatalogRepository(backend))

        val page = assertIs<CatalogPage.Loaded>(pager.first(AllTypes))

        assertEquals(listOf(ItemType.MOVIE, ItemType.SERIES, ItemType.DOCUMENTARY), page.items.take(3).map { it.type })
        assertEquals(AllTypes, pager.loaded)
        assertFalse(pager.endReached)
    }

    @Test
    fun `count is an estimate until the last page and exact after it`() = runTest {
        val pager = CatalogPager(FakeCatalogRepository(backend))
        val first = assertIs<CatalogPage.Loaded>(pager.first(AllTypes))

        assertEquals(CatalogCount(ESTIMATED_ALL, exact = false), pager.count(first.items.size))

        val next = assertIs<CatalogPage.Loaded>(pager.next())
        val shown = first.items.size + next.items.size

        assertTrue(pager.endReached)
        assertEquals(CatalogCount(ALL_TITLES, exact = true), pager.count(shown))
        assertTrue(next.items.all { it.type == ItemType.MOVIE }, "исчерпанные типы запросились снова")
    }

    @Test
    fun `next page after the end is empty`() = runTest {
        val pager = CatalogPager(FakeCatalogRepository(backend))
        pager.first(OnlySeries)

        assertTrue(pager.endReached)
        assertEquals(CatalogPage.Loaded(emptyList()), pager.next())
    }

    @Test
    fun `same query again is unchanged and not requested`() = runTest {
        val catalog = RecordingCatalog(FakeCatalogRepository(backend))
        val pager = CatalogPager(catalog)
        pager.first(AllTypes)
        val requests = catalog.requests.size

        assertEquals(CatalogPage.Unchanged, pager.first(AllTypes))
        assertEquals(requests, catalog.requests.size)
    }

    @Test
    fun `answer for a query that was replaced meanwhile is outdated`() = runTest {
        val catalog = RecordingCatalog(FakeCatalogRepository(backend)) { type ->
            if (type == ItemType.SERIES) SLOW_ANSWER_MS else 0L
        }
        val pager = CatalogPager(catalog)

        val slow = async { pager.first(OnlySeries) }
        val fast = async { pager.first(OnlyMovies) }

        assertIs<CatalogPage.Loaded>(fast.await())
        assertEquals(CatalogPage.Outdated, slow.await())
        assertEquals(OnlyMovies, pager.loaded)
    }

    @Test
    fun `failed first page forgets the loaded query so retry goes to the network`() = runTest {
        val catalog = RecordingCatalog(FakeCatalogRepository(backend))
        val pager = CatalogPager(catalog)
        pager.first(AllTypes)

        catalog.offline = true
        assertEquals(CatalogPage.Failed(RecordingCatalog.OFFLINE_MESSAGE), pager.first(OnlyMovies))
        assertNull(pager.loaded)

        catalog.offline = false
        assertIs<CatalogPage.Loaded>(pager.first(OnlyMovies))
    }

    private companion object {
        val ByViews = SortOption(CatalogSort.VIEWS)
        val AllTypes = CatalogQuery(
            types = listOf(ItemType.MOVIE, ItemType.SERIES, ItemType.DOCUMENTARY),
            genreId = null,
            filters = CatalogFilters(),
            sort = ByViews,
        )
        val OnlySeries = AllTypes.copy(types = listOf(ItemType.SERIES))
        val OnlyMovies = AllTypes.copy(types = listOf(ItemType.MOVIE))

        const val ESTIMATED_ALL = 62
        const val ALL_TITLES = 49
        const val SLOW_ANSWER_MS = 1_000L
    }
}

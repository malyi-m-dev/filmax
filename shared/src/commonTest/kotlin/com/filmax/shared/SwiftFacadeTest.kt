package com.filmax.shared

import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.GenreFilter
import com.filmax.core.domain.catalog.ReferenceGenre
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.RatingSource
import com.filmax.core.domain.common.getOrNull
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeCatalogRepository
import com.filmax.data.fake.FakeIds
import com.filmax.data.fake.FakeLatency
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Фасад для Swift: запрос витрины как у Android и тематические ряды Главной. */
class SwiftFacadeTest {

    private val byViews = SortOption(CatalogSort.VIEWS, ascending = false)

    @Test
    fun `all chip is movies, series and documentaries, anime is a genre over movies and series`() {
        val all = CatalogKit.query(type = null, genreId = 9, filters = CatalogKit.noFilters, sort = byViews)
        val anime = CatalogKit.query(type = ItemType.ANIME, genreId = 9, filters = CatalogKit.noFilters, sort = byViews)

        assertEquals(listOf(ItemType.MOVIE, ItemType.SERIES, ItemType.DOCUMENTARY), all.types)
        assertEquals(GenreFilter.Listed(9), all.genre)
        assertEquals(listOf(ItemType.MOVIE, ItemType.SERIES), anime.types)
        assertEquals(GenreFilter.Reference(ReferenceGenre.ANIME), anime.genre)
    }

    @Test
    fun `filters are edited one field at a time`() {
        val filters = CatalogKit.withMinRating(
            CatalogKit.withYears(CatalogKit.noFilters, from = 2010, to = null),
            RatingSource.COMMUNITY,
            value = 7,
        )

        assertEquals(2010, filters.yearFrom)
        assertNull(filters.yearTo)
        assertEquals(7, CatalogKit.minRating(filters, RatingSource.COMMUNITY))
        val cleared = CatalogKit.withMinRating(filters, RatingSource.COMMUNITY, value = null)
        assertNull(CatalogKit.minRating(cleared, RatingSource.COMMUNITY))
    }

    @Test
    fun `anime shelf holds only anime titles`() = runTest {
        val shelves = HomeShelves(FakeCatalogRepository(FakeBackend(latency = FakeLatency.None)))

        val items = shelves.load(HomeShelf.ANIME, page = 1).getOrNull()?.items.orEmpty()

        assertTrue(items.isNotEmpty())
        assertTrue(items.all { item -> item.genres.any { it.id == FakeIds.ANIME_GENRE } })
    }
}

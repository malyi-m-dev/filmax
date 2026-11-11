package com.filmax.core.domain.catalog

import com.filmax.core.domain.catalog.model.Duration
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemRating
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.Posters
import com.filmax.core.domain.catalog.model.RatingSource
import kotlin.test.Test
import kotlin.test.assertEquals

/** Локальная обработка выдачи: фильтры поиска, повторы id между страницами и досортировка. */
class ArrangeCatalogTest {

    private val comedy = Genre(id = 1, title = "Комедия")

    @Test
    fun `search results keep the genre and rating threshold of the screen`() {
        val items = listOf(
            item(1, genres = listOf(comedy), rating = ItemRating.of(RatingSource.KINOPOISK to "8.0")),
            item(2, genres = listOf(comedy), rating = ItemRating.of(RatingSource.KINOPOISK to "6.0")),
            item(3, rating = ItemRating.of(RatingSource.KINOPOISK to "9.0")),
        )
        val filters = CatalogFilters().withMinRating(RatingSource.KINOPOISK, 7)

        val arranged = ArrangeCatalog.searchResults(items, genreId = comedy.id, filters, SortOption(CatalogSort.VIEWS))

        assertEquals(listOf(1), arranged.map { it.id })
    }

    @Test
    fun `threshold of the home server rating does not empty a catalog without kinopoisk`() {
        val items = listOf(item(1, rating = ItemRating.of(RatingSource.COMMUNITY to "7.5")))
        val filters = CatalogFilters().withMinRating(RatingSource.COMMUNITY, 7)

        val arranged = ArrangeCatalog.searchResults(items, genreId = null, filters, SortOption(CatalogSort.VIEWS))

        assertEquals(listOf(1), arranged.map { it.id })
    }

    @Test
    fun `next page skips titles already in the grid`() {
        val shown = listOf(item(1), item(2))
        val page = listOf(item(2), item(3), item(3))

        val merged = ArrangeCatalog.nextPage(shown, page, SortOption(CatalogSort.VIEWS))

        assertEquals(listOf(1, 2, 3), merged.map { it.id })
    }

    @Test
    fun `rating and year are resorted locally, other keys keep the source order`() {
        val items = listOf(item(1, year = 2001), item(2, year = 2020), item(3, year = 2010))

        assertEquals(listOf(2, 3, 1), ArrangeCatalog.firstPage(items, SortOption(CatalogSort.YEAR)).map { it.id })
        assertEquals(
            listOf(1, 3, 2),
            ArrangeCatalog.firstPage(items, SortOption(CatalogSort.YEAR, ascending = true)).map { it.id },
        )
        assertEquals(listOf(1, 2, 3), ArrangeCatalog.firstPage(items, SortOption(CatalogSort.VIEWS)).map { it.id })
    }

    private fun item(
        id: Int,
        year: Int = 2020,
        genres: List<Genre> = emptyList(),
        rating: ItemRating = ItemRating(),
    ) = Item(
        id = id,
        title = "Тайтл $id",
        type = ItemType.MOVIE,
        year = year,
        plot = "",
        director = "",
        cast = "",
        country = "",
        genres = genres,
        rating = rating,
        posters = Posters(small = "", medium = "", big = "", wide = null),
        duration = Duration(averageMinutes = null, totalMinutes = null),
        tracklist = emptyList(),
        trailer = null,
        inWatchlist = false,
        finished = false,
    )
}

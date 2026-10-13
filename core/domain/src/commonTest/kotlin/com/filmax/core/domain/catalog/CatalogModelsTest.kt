package com.filmax.core.domain.catalog

import com.filmax.core.domain.catalog.model.ItemRating
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.Pagination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CatalogModelsTest {

    @Test
    fun `external rating averages IMDb and KinoPoisk`() {
        val rating = ItemRating(filmax = 0, filmaxPercentage = "", imdb = "8.0", kinopoisk = "7.0")

        assertEquals(7.5, rating.external)
    }

    @Test
    fun `external rating uses the only score available and ignores garbage`() {
        val onlyImdb = ItemRating(filmax = 0, filmaxPercentage = "", imdb = "8.1", kinopoisk = null)
        val garbageKinopoisk = ItemRating(filmax = 0, filmaxPercentage = "", imdb = "6.0", kinopoisk = "—")

        assertEquals(8.1, onlyImdb.external)
        assertEquals(6.0, garbageKinopoisk.external)
    }

    @Test
    fun `external rating is null without any score`() {
        assertNull(ItemRating(filmax = 0, filmaxPercentage = "", imdb = null, kinopoisk = null).external)
    }

    @Test
    fun `sort goes descending by default and ascending on request`() {
        assertEquals("-rating", SortOption(CatalogSort.RATING).apiValue)
        assertEquals("year", SortOption(CatalogSort.YEAR, ascending = true).apiValue)
    }

    @Test
    fun `active filters count a year range once`() {
        assertEquals(0, CatalogFilters().activeCount)
        assertEquals(1, CatalogFilters(yearFrom = 2010, yearTo = 2020).activeCount)
        assertEquals(3, CatalogFilters(yearFrom = 2010, only4k = true, onlyFinished = false).activeCount)
    }

    @Test
    fun `next page exists until the current page reaches the total`() {
        assertTrue(Pagination(total = 3, current = 2, perPage = 20).hasNextPage)
        assertFalse(Pagination(total = 3, current = 3, perPage = 20).hasNextPage)
    }

    @Test
    fun `unknown item type falls back to a movie`() {
        assertEquals(ItemType.SERIES, ItemType.from("serial"))
        assertEquals(ItemType.MOVIE, ItemType.from("3d"))
    }
}

package com.filmax.core.domain.catalog

import com.filmax.core.domain.catalog.model.ItemRating
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.Pagination
import com.filmax.core.domain.catalog.model.RatingSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CatalogModelsTest {

    @Test
    fun `external rating averages every score`() {
        val rating = ItemRating.of(RatingSource.KINOPOISK to "7.0", RatingSource.IMDB to "8.0")

        assertEquals(7.5, rating.external)
    }

    @Test
    fun `external rating uses the only score available and ignores garbage`() {
        val onlyImdb = ItemRating.of(RatingSource.KINOPOISK to null, RatingSource.IMDB to "8.1")
        val garbageKinopoisk = ItemRating.of(RatingSource.KINOPOISK to "—", RatingSource.IMDB to "6.0")

        assertEquals(8.1, onlyImdb.external)
        assertEquals(6.0, garbageKinopoisk.external)
    }

    @Test
    fun `external rating is null without any score`() {
        assertNull(ItemRating().external)
    }

    @Test
    fun `primary rating is the first score of the source`() {
        val kinopub = ItemRating.of(RatingSource.KINOPOISK to "7.9", RatingSource.IMDB to "8.4")
        val homeServer = ItemRating.of(RatingSource.COMMUNITY to "6.5")

        assertEquals(7.9, kinopub.primary)
        assertEquals(8.4, kinopub.of(RatingSource.IMDB))
        assertEquals(6.5, homeServer.primary)
        assertNull(homeServer.of(RatingSource.KINOPOISK))
    }

    @Test
    fun `rating thresholds count once each`() {
        val filters = CatalogFilters()
            .withMinRating(RatingSource.KINOPOISK, 7)
            .withMinRating(RatingSource.IMDB, 8)

        assertEquals(2, filters.activeCount)
        assertEquals(1, filters.withMinRating(RatingSource.IMDB, null).activeCount)
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
    fun `tv shows and documentary series are episodic, concerts and documentary films are not`() {
        assertTrue(ItemType.TV_SHOW.episodic)
        assertTrue(ItemType.DOCUMENTARY.episodic)
        assertFalse(ItemType.CONCERT.episodic)
        assertFalse(ItemType.DOCUMENTARY_MOVIE.episodic)
    }
}

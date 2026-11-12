package com.filmax.core.presentation.cards

import com.filmax.core.domain.catalog.model.Duration
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemRating
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.Posters
import com.filmax.core.domain.catalog.model.Rating
import com.filmax.core.domain.catalog.model.RatingSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TitleCardTest {

    @Test
    fun `a movie premieres, a series gets a new episode`() {
        assertEquals("Премьера", item(type = ItemType.MOVIE).toTitleCard().freshBadge)
        assertEquals("Новинка", item(type = ItemType.SERIES).toTitleCard().freshBadge)
    }

    @Test
    fun `length is shown for a movie only`() {
        assertEquals("2021 · 1 ч 52 мин", item(type = ItemType.MOVIE).toTitleCard().yearAndLength)
        assertEquals("2021", item(type = ItemType.SERIES).toTitleCard().yearAndLength)
    }

    @Test
    fun `rating from eight is highlighted, a missing one is not drawn`() {
        val high = item(rating = 8.2).toTitleCard()
        assertEquals("8.2", high.rating)
        assertTrue(high.ratingHigh)
        val missing = item(rating = null).toTitleCard()
        assertNull(missing.rating)
        assertFalse(missing.ratingHigh)
    }

    @Test
    fun `missing posters fall back to the bigger ones`() {
        val card = item(posters = Posters(small = "s", medium = "", big = "b", wide = null)).toTitleCard()
        assertEquals("b", card.posterUrl)
        assertEquals("b", card.widePosterUrl)
    }

    private fun item(
        type: ItemType = ItemType.MOVIE,
        rating: Double? = 7.0,
        posters: Posters = Posters(small = "s", medium = "m", big = "b", wide = "w"),
    ) = Item(
        id = 1,
        title = "Дом на краю карты",
        type = type,
        year = 2021,
        plot = "",
        director = "",
        cast = "",
        country = "",
        genres = emptyList(),
        rating = ItemRating(listOfNotNull(rating?.let { Rating(RatingSource.KINOPOISK, it) })),
        posters = posters,
        duration = Duration(averageMinutes = 112.0, totalMinutes = 112),
        tracklist = emptyList(),
        trailer = null,
        inWatchlist = false,
        finished = false,
    )
}

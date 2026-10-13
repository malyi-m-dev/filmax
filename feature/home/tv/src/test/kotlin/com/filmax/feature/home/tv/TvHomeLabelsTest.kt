package com.filmax.feature.home.tv

import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.ui.components.isHighRating
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TvHomeLabelsTest {

    @Test
    fun `collection caption declines the word for films`() {
        assertEquals("1 фильм", collectionCaption(1))
        assertEquals("3 фильма", collectionCaption(3))
        assertEquals("5 фильмов", collectionCaption(5))
        assertEquals("11 фильмов", collectionCaption(11))
        assertEquals("21 фильм", collectionCaption(21))
        assertEquals("24 фильма", collectionCaption(24))
        assertEquals("112 фильмов", collectionCaption(112))
    }

    @Test
    fun `scroll thumb follows the first visible row`() {
        assertEquals(ScrollThumb.Whole, scrollThumb(position = 0f, visibleCount = 4, total = 4))
        assertEquals(ScrollThumb(start = 0f, length = 0.25f), scrollThumb(position = 0f, visibleCount = 2, total = 8))
        assertEquals(ScrollThumb(start = 0.5f, length = 0.25f), scrollThumb(position = 3f, visibleCount = 2, total = 8))
        assertEquals(1f, scrollThumb(position = 7f, visibleCount = 2, total = 8).start)
    }

    @Test
    fun `collection caption is absent when the count is unknown`() {
        assertNull(collectionCaption(null))
    }

    @Test
    fun `rating is high from eight`() {
        assertTrue(isHighRating("8.0"))
        assertTrue(isHighRating("9.1"))
        assertFalse(isHighRating("7.9"))
        assertFalse(isHighRating(null))
        assertFalse(isHighRating("—"))
    }

    @Test
    fun `fresh badge depends on the title type`() {
        assertEquals("Премьера", freshBadge(ItemType.MOVIE))
        assertEquals("Новинка", freshBadge(ItemType.SERIES))
    }
}

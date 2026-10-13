package com.filmax.feature.collections.tv

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TvCollectionLabelsTest {

    @Test
    fun `caption agrees the word with the number`() {
        assertEquals("1 фильм", titlesCaption(1))
        assertEquals("24 фильма", titlesCaption(24))
        assertEquals("45 фильмов", titlesCaption(45))
        assertEquals("11 фильмов", titlesCaption(11))
        assertEquals("112 фильмов", titlesCaption(112))
        assertEquals("101 фильм", titlesCaption(101))
        assertNull(titlesCaption(null))
    }

    @Test
    fun `cards fall into rows of six`() {
        assertEquals(0, gridRow(0))
        assertEquals(0, gridRow(COLLECTION_COLUMNS - 1))
        assertEquals(1, gridRow(COLLECTION_COLUMNS))
        assertEquals(2, gridRow(COLLECTION_COLUMNS * 2 + 3))
    }

    @Test
    fun `scroll thumb covers the visible share and moves with the first row`() {
        assertEquals(ScrollThumb(start = 0f, length = 1f), scrollThumb(firstVisible = 0, visibleCount = 12, total = 12))
        assertEquals(
            ScrollThumb(start = 0f, length = 0.25f),
            scrollThumb(firstVisible = 0, visibleCount = 12, total = 48)
        )
        assertEquals(
            ScrollThumb(start = 1f, length = 0.25f),
            scrollThumb(firstVisible = 36, visibleCount = 12, total = 48)
        )
        assertEquals(ScrollThumb(start = 0f, length = 1f), scrollThumb(firstVisible = 0, visibleCount = 0, total = 0))
    }
}

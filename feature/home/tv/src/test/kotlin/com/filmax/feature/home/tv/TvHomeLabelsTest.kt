package com.filmax.feature.home.tv

import kotlin.test.Test
import kotlin.test.assertEquals

class TvHomeLabelsTest {

    @Test
    fun `scroll thumb follows the first visible row`() {
        assertEquals(ScrollThumb.Whole, scrollThumb(position = 0f, visibleCount = 4, total = 4))
        assertEquals(ScrollThumb(start = 0f, length = 0.25f), scrollThumb(position = 0f, visibleCount = 2, total = 8))
        assertEquals(ScrollThumb(start = 0.5f, length = 0.25f), scrollThumb(position = 3f, visibleCount = 2, total = 8))
        assertEquals(1f, scrollThumb(position = 7f, visibleCount = 2, total = 8).start)
    }
}

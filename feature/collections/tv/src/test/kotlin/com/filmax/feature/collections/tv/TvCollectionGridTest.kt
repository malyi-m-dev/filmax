package com.filmax.feature.collections.tv

import kotlin.test.Test
import kotlin.test.assertEquals

class TvCollectionGridTest {

    @Test
    fun `cards fall into rows of six`() {
        assertEquals(0, gridRow(0))
        assertEquals(0, gridRow(COLLECTION_COLUMNS - 1))
        assertEquals(1, gridRow(COLLECTION_COLUMNS))
        assertEquals(2, gridRow(COLLECTION_COLUMNS * 2 + 3))
    }
}

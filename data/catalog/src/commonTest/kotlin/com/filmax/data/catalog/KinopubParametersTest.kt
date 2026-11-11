package com.filmax.data.catalog

import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.ItemType
import kotlin.test.Test
import kotlin.test.assertEquals

/** Доменные понятия → параметры kino.pub: эти строки уходят в запрос как есть. */
class KinopubParametersTest {

    @Test
    fun `sort goes descending by default and ascending on request`() {
        assertEquals("-rating", SortOption(CatalogSort.RATING).kinopubSort)
        assertEquals("year", SortOption(CatalogSort.YEAR, ascending = true).kinopubSort)
    }

    @Test
    fun `unknown item type falls back to a movie`() {
        assertEquals(ItemType.SERIES, kinopubItemType("serial"))
        assertEquals(ItemType.MOVIE, kinopubItemType("3d"))
        assertEquals(ItemType.MOVIE, kinopubItemType(null))
    }

    @Test
    fun `every type round-trips through its kinopub value`() {
        ItemType.entries.forEach { type -> assertEquals(type, kinopubItemType(type.kinopubValue)) }
    }
}

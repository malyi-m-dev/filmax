package com.filmax.feature.search.common

import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.CatalogSection
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.model.Country
import com.filmax.core.domain.catalog.model.ItemType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Разделы каталога с Главной и чипы применённых фильтров над сеткой. */
class CatalogSectionsTest {

    @Test
    fun `section sets the starting selection, no section keeps the default storefront`() {
        assertEquals(ItemType.MOVIE, SearchState().withSection(CatalogSection.MOVIES).filter)
        assertEquals(ItemType.ANIME, SearchState().withSection(CatalogSection.ANIME).filter)
        assertEquals(ItemType.TV_SHOW, SearchState().withSection(CatalogSection.TV_SHOWS).filter)
        assertEquals(ItemType.CONCERT, SearchState().withSection(CatalogSection.CONCERTS).filter)
        assertTrue(SearchState().withSection(CatalogSection.UHD).filters.only4k)
        assertEquals(CatalogSort.CREATED, SearchState().withSection(CatalogSection.FRESH).sort.field)
        assertEquals(SearchState(), SearchState().withSection(null))
    }

    @Test
    fun `section name from navigation is read back, an unknown one is ignored`() {
        assertEquals(CatalogSection.UHD, CatalogSection.from("UHD"))
        assertNull(CatalogSection.from("CARTOONS"))
        assertNull(CatalogSection.from(null))
    }

    @Test
    fun `every applied filter is its own chip and its cross removes only it`() {
        val filters = CatalogFilters(yearFrom = 2010, yearTo = 2020, kpRatingFrom = 7, countryId = 3, only4k = true)

        val chips = filters.applied(listOf(Country(3, "Франция")))

        assertEquals(listOf("2010–2020", "КП 7+", "Франция", "4K"), chips.map { it.label })
        assertEquals(filters.copy(kpRatingFrom = null), chips[1].without)
        assertEquals(filters.copy(yearFrom = null, yearTo = null), chips[0].without)
    }

    @Test
    fun `open year range and unknown country still read as words`() {
        assertEquals(listOf("С 2015"), CatalogFilters(yearFrom = 2015).applied(emptyList()).map { it.label })
        assertEquals(listOf("До 1999"), CatalogFilters(yearTo = 1999).applied(emptyList()).map { it.label })
        assertEquals(listOf("Страна"), CatalogFilters(countryId = 9).applied(emptyList()).map { it.label })
        assertTrue(CatalogFilters().applied(emptyList()).isEmpty())
    }
}

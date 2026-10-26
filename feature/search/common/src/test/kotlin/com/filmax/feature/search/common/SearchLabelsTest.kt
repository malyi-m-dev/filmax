package com.filmax.feature.search.common

import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.CatalogSection
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.Country
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.ItemType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Подписи каталога и поиска: сводка со счётчиком, чипы, применённые фильтры, разделы с Главной. */
class SearchLabelsTest {

    @Test
    fun `section sets the starting selection and the title, no section keeps the default storefront`() {
        assertEquals(ItemType.MOVIE, SearchState().withSection(CatalogSection.MOVIES).filter)
        assertEquals(ItemType.ANIME, SearchState().withSection(CatalogSection.ANIME).filter)
        assertEquals(ItemType.TV_SHOW, SearchState().withSection(CatalogSection.TV_SHOWS).filter)
        assertEquals(ItemType.CONCERT, SearchState().withSection(CatalogSection.CONCERTS).filter)
        assertTrue(SearchState().withSection(CatalogSection.UHD).filters.only4k)
        assertEquals(CatalogSort.CREATED, SearchState().withSection(CatalogSection.FRESH).sort.field)
        assertEquals("4K и HDR", SearchState().withSection(CatalogSection.UHD).title)
        assertEquals(SearchState(), SearchState().withSection(null))
        assertEquals("Каталог", SearchState().title)
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

        val chips = appliedFilters(filters, listOf(Country(3, "Франция")))

        assertEquals(listOf("2010–2020", "КП 7+", "Франция", "4K"), chips.map { it.label })
        assertEquals(filters.copy(kpRatingFrom = null), chips[1].without)
        assertEquals(filters.copy(yearFrom = null, yearTo = null), chips[0].without)
    }

    @Test
    fun `open year range and unknown country still read as words`() {
        assertEquals(listOf("С 2015"), appliedFilters(CatalogFilters(yearFrom = 2015), emptyList()).map { it.label })
        assertEquals(listOf("До 1999"), appliedFilters(CatalogFilters(yearTo = 1999), emptyList()).map { it.label })
        assertEquals(listOf("Страна"), appliedFilters(CatalogFilters(countryId = 9), emptyList()).map { it.label })
        assertTrue(appliedFilters(CatalogFilters(), emptyList()).isEmpty())
    }

    @Test
    fun `exact count uses the nominative plural`() {
        assertEquals("1 результат", countLabel(CatalogCount(1, exact = true)))
        assertEquals("42 результата", countLabel(CatalogCount(42, exact = true)))
        assertEquals("49 результатов", countLabel(CatalogCount(49, exact = true)))
        assertEquals("11 результатов", countLabel(CatalogCount(11, exact = true)))
    }

    @Test
    fun `estimate says about and takes the genitive`() {
        assertEquals("около 62 результатов", countLabel(CatalogCount(62, exact = false)))
        assertEquals("около 21 результата", countLabel(CatalogCount(21, exact = false)))
        assertEquals("около 111 результатов", countLabel(CatalogCount(111, exact = false)))
    }

    @Test
    fun `summary names type and genre but not the all chip`() {
        val sciFi = Genre(id = 4, title = "Фантастика", type = "movie")
        val state =
            SearchState(genres = listOf(sciFi), selectedGenreId = 4, catalogCount = CatalogCount(42, exact = true))

        assertEquals("Фантастика · 42 результата", catalogSummary(state))
        assertEquals("Сериалы · Фантастика · 42 результата", catalogSummary(state.copy(filter = ItemType.SERIES)))
        assertEquals("", catalogSummary(SearchState()))
    }

    @Test
    fun `more results label agrees with the number`() {
        assertEquals("Ещё 1 результат", moreResultsLabel(1))
        assertEquals("Ещё 3 результата", moreResultsLabel(3))
        assertEquals("Ещё 6 результатов", moreResultsLabel(6))
    }

    @Test
    fun `sort chip shows an arrow only for the unusual order`() {
        assertEquals("Просмотры", sortChipLabel(SortOption(CatalogSort.VIEWS)))
        assertEquals("Год ↑", sortChipLabel(SortOption(CatalogSort.YEAR, ascending = true)))
    }

    @Test
    fun `filters chip counts applied filters`() {
        assertEquals("Фильтры", filtersChipLabel(CatalogFilters()))
        assertEquals("Фильтры · 2", filtersChipLabel(CatalogFilters(only4k = true, kpRatingFrom = 7)))
    }

    @Test
    fun `search hint tells to type, to wait or that nothing was found`() {
        assertEquals("Наберите название — результаты появятся здесь", searchHint(SearchState(query = "х ")))
        assertEquals("Ищем…", searchHint(SearchState(query = "хрони", loading = true)))
        assertEquals("По запросу «хрони» ничего не нашлось", searchHint(SearchState(query = "хрони")))
    }
}

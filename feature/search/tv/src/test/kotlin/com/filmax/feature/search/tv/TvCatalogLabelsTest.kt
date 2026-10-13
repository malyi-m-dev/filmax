package com.filmax.feature.search.tv

import androidx.compose.ui.graphics.Color
import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.ui.components.VoiceSearchError
import com.filmax.feature.search.common.CatalogCount
import com.filmax.feature.search.common.SearchState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Подписи каталога: сводка со счётчиком, чипы, «Ещё N», подсветка совпадения, подсказки поиска. */
class TvCatalogLabelsTest {

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
    fun `title highlights the typed part in the accent color`() {
        val title = highlightedTitle("Хроники Пограничья", "хрони", Accent)

        assertEquals("Хроники Пограничья", title.text)
        val span = title.spanStyles.single()
        assertEquals(0 until 5, span.start until span.end)
        assertEquals(Accent, span.item.color)
    }

    @Test
    fun `title without a match is left as is`() {
        assertTrue(highlightedTitle("Хроники Пограничья", "нильсен", Accent).spanStyles.isEmpty())
    }

    @Test
    fun `search hint tells to type, to wait or that nothing was found`() {
        assertEquals("Наберите название — результаты появятся здесь", searchHint(SearchState(query = "х ")))
        assertEquals("Ищем…", searchHint(SearchState(query = "хрони", loading = true)))
        assertEquals("По запросу «хрони» ничего не нашлось", searchHint(SearchState(query = "хрони")))
    }

    @Test
    fun `load more skeletons fill the last row and add one more`() {
        assertEquals(CATALOG_COLUMNS, loadMoreSkeletons(40))
        assertEquals(3 + CATALOG_COLUMNS, loadMoreSkeletons(42))
    }

    @Test
    fun `voice errors are explained in words`() {
        assertTrue("интернет" in voiceErrorText(VoiceSearchError.NETWORK))
        assertTrue("микрофон" in voiceErrorText(VoiceSearchError.PERMISSION))
    }

    private companion object {
        val Accent = Color(0xFFBEB2FF)
    }
}

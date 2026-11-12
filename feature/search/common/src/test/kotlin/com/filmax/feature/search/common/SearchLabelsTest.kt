package com.filmax.feature.search.common

import com.filmax.core.domain.catalog.CatalogCount
import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.CatalogSection
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.RatingSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Готовые подписи Каталога и Поиска: счётчик, сводка, чипы, заголовок раздела, подсказки. */
class SearchLabelsTest {

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
        val sciFi = Genre(id = 4, title = "Фантастика")
        val state = SearchState(
            genres = listOf(sciFi),
            selectedGenreId = 4,
            catalogCount = CatalogCount(42, exact = true),
        )

        assertEquals("Фантастика · 42 результата", state.catalogSummary)
        assertEquals("Сериалы · Фантастика · 42 результата", state.copy(filter = ItemType.SERIES).catalogSummary)
        assertEquals("", SearchState().catalogSummary)
    }

    @Test
    fun `sort chip shows an arrow only for the unusual order`() {
        assertEquals("Просмотры", SearchState(sort = SortOption(CatalogSort.VIEWS)).sortChip)
        assertEquals("Год ↑", SearchState(sort = SortOption(CatalogSort.YEAR, ascending = true)).sortChip)
    }

    @Test
    fun `filters chip counts applied filters`() {
        assertEquals("Фильтры", SearchState().filtersChip)
        val filters = CatalogFilters(only4k = true).withMinRating(RatingSource.KINOPOISK, 7)

        assertEquals("Фильтры · 2", SearchState(filters = filters).filtersChip)
    }

    @Test
    fun `search hint tells to type, to wait or that nothing was found`() {
        assertEquals("Наберите название — результаты появятся здесь", SearchState(query = "х ").searchHint)
        assertEquals("Ищем…", SearchState(query = "хрони", loading = true).searchHint)
        assertEquals("По запросу «хрони» ничего не нашлось", SearchState(query = "хрони").searchHint)
    }

    @Test
    fun `title is the section opened from home, otherwise the tab`() {
        assertEquals("Каталог", SearchState().title)
        assertEquals("4K и HDR", SearchState().withSection(CatalogSection.UHD).title)
    }

    @Test
    fun `genre is not offered for anime — its genre is the type itself`() {
        val genres = listOf(Genre(id = 4, title = "Фантастика"))

        assertTrue(SearchState(genres = genres).genrePickable)
        assertFalse(SearchState(genres = genres, filter = ItemType.ANIME).genrePickable)
        assertFalse(SearchState().genrePickable)
        assertNull(SearchState(genres = genres).selectedGenre)
    }
}

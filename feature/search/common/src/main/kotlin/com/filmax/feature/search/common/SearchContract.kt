package com.filmax.feature.search.common

import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.Country
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType

/** С этой длины запроса имеет смысл идти в поиск — короче отдаётся мусор релевантности. */
const val MIN_QUERY_LENGTH = 2

data class SearchState(
    val query: String = "",
    val filter: ItemType? = null,
    /** Сортировка выдачи каталога: поле + направление. VIEWS = «Просмотры» — дефолт витрины. */
    val sort: SortOption = SortOption(CatalogSort.VIEWS),
    /** Диапазонные фильтры каталога (год, рейтинги, страна, 4K, завершённость). */
    val filters: CatalogFilters = CatalogFilters(),
    /** Реальные жанры из API. Пусты, пока экран не запросил [SearchEvent.LoadCatalog]. */
    val genres: List<Genre> = emptyList(),
    val selectedGenreId: Int? = null,
    /** Страны для фильтра. Пусты, пока экран не запросил [SearchEvent.LoadCatalog]. */
    val countries: List<Country> = emptyList(),
    /** Результаты поиска по [query]. Пусты, пока запрос короче [MIN_QUERY_LENGTH]. */
    val results: List<Item> = emptyList(),
    /**
     * Выдача каталога по фильтрам без запроса. Живёт отдельно от [results], потому что на
     * телефоне пустой запрос значит «показать подсказки», а не «показать витрину».
     */
    val catalogItems: List<Item> = emptyList(),
    /**
     * Включается экраном, которому витрина нужна (TV-Каталог). Без этого флага телефонный
     * поиск на каждом открытии тянул бы выдачу, которую никогда не рисует.
     */
    val catalogEnabled: Boolean = false,
    val recentQueries: List<String> = emptyList(),
    val trendingQueries: List<String> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
) {
    /** Что показывает сетка каталога: поиск — с [MIN_QUERY_LENGTH] символов, иначе витрина. */
    val visibleItems: List<Item>
        get() = if (query.length >= MIN_QUERY_LENGTH) results else catalogItems
}

sealed interface SearchEvent {
    data class QueryChange(val query: String) : SearchEvent
    data class FilterChange(val filter: ItemType?) : SearchEvent

    /** Смена сортировки целиком (поле и/или направление) — UI собирает [SortOption] сам. */
    data class SortChange(val sort: SortOption) : SearchEvent

    /** null — снять фильтр по жанру. */
    data class GenreChange(val genreId: Int?) : SearchEvent

    /** Применить диапазонные фильтры (лист «Фильтры» на телефоне, чипы на TV). */
    data class ApplyFilters(val filters: CatalogFilters) : SearchEvent

    /** Сбросить диапазонные фильтры в дефолт (кнопка «Сбросить»). Сортировку не трогает. */
    data object ResetFilters : SearchEvent

    /** Тап по подсказке (тренды/недавние): подставить запрос и сразу искать. */
    data class SubmitQuery(val query: String) : SearchEvent
    data object ClearRecent : SearchEvent

    /** Экран-витрина заявляет о себе: подтянуть жанры, страны и выдачу по текущим фильтрам. */
    data object LoadCatalog : SearchEvent
}

sealed interface SearchSideEffect

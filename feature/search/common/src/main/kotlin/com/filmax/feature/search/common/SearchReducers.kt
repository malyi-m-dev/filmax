// Чистые переходы состояния поиска и витрины: выборка, панели ТВ, недавние запросы и запрос витрины
// по чипам. Модель (SearchScreenModel) только применяет их и ходит в сеть.
package com.filmax.feature.search.common

import com.filmax.core.domain.catalog.ArrangeCatalog
import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.CatalogQuery
import com.filmax.core.domain.catalog.GenreFilter
import com.filmax.core.domain.catalog.ReferenceGenre
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType

private const val RECENT_LIMIT = 8

/**
 * Что показывает чип «Все». `api/v1/items` без параметра `type` не ходит, поэтому «все» —
 * это объединение конкретных типов; ItemType.TV (эфирные каналы) в витрину не входит.
 * ANIME здесь нет: у kino.pub нет такого типа (api/v1/types), и аниме-тайтлы уже входят
 * в выдачу как movie/serial со своим жанром.
 */
private val BrowseTypes = listOf(ItemType.MOVIE, ItemType.SERIES, ItemType.DOCUMENTARY)

/**
 * Чип «Аниме»: такого типа у источников нет — аниме это ЖАНР поверх фильмов и сериалов. Поэтому
 * фильтр ANIME разворачивается в фильмы+сериалы с опорным жанром [ReferenceGenre.ANIME];
 * выбранный в ряду жанр на это время игнорируется — жанр в запросе один.
 */
private val AnimeTypes = listOf(ItemType.MOVIE, ItemType.SERIES)

/** Выборка после события: тип, сортировка, жанр, фильтры. Панель закрывает отдельное событие. */
internal fun SearchState.withSelection(event: SearchEvent.Selection): SearchState = when (event) {
    is SearchEvent.FilterChange -> copy(filter = event.filter)
    is SearchEvent.SortChange -> copy(sort = event.sort)
    is SearchEvent.GenreChange -> copy(selectedGenreId = event.genreId)
    is SearchEvent.ApplyFilters -> copy(filters = event.filters)
    SearchEvent.ResetFilters -> copy(filters = CatalogFilters())
}

/**
 * Панели каталога. Панель фильтров открывается с черновиком, равным применённым фильтрам: закрытая
 * без «Применить», она не оставляет следов, а «Применить» переносит черновик в выдачу.
 */
internal fun SearchState.withSheet(event: SearchEvent.Sheet): SearchState = when (event) {
    is SearchEvent.OpenSheet -> copy(sheetOpen = true, sheet = event.sheet, filtersDraft = filters)
    SearchEvent.CloseSheet -> copy(sheetOpen = false)
    is SearchEvent.EditFilters -> copy(filtersDraft = event.filters)
    SearchEvent.ResetDraftFilters -> copy(filtersDraft = CatalogFilters())
    SearchEvent.ApplyDraftFilters -> copy(filters = filtersDraft)
}

/**
 * Тип для запроса поиска. На ТВ поиск — отдельный режим без чипов каталога: фильтр, которого
 * зритель не видит, тихо прятал бы найденное, поэтому там ищем по всему каталогу.
 */
internal fun SearchState.searchType(): ItemType? = if (searchOpen) null else filter

/** Выдача витрины по фильтрам экрана; «Аниме» — жанр поверх фильмов и сериалов. */
internal fun SearchState.catalogQuery(): CatalogQuery = CatalogQuery(
    types = when (filter) {
        null -> BrowseTypes
        ItemType.ANIME -> AnimeTypes
        else -> listOf(filter)
    },
    genre = if (filter == ItemType.ANIME) {
        GenreFilter.Reference(ReferenceGenre.ANIME)
    } else {
        selectedGenreId?.let(GenreFilter::Listed)
    },
    filters = filters,
    sort = sort,
)

/** Запрос в начало недавних — если он годится для поиска. */
internal fun SearchState.withRecent(query: String): SearchState {
    val trimmed = query.trim()
    if (trimmed.length < MIN_QUERY_LENGTH) return this
    return copy(recentQueries = (listOf(trimmed) + recentQueries).distinct().take(RECENT_LIMIT))
}

/**
 * Жанр, фильтры и сортировка поверх выдачи поиска (см. [ArrangeCatalog]). На ТВ поиск — отдельный
 * режим без чипов каталога: там только убираем повторы id, иначе сетка падает на дубле ключа.
 */
internal fun SearchState.arranged(items: List<Item>): List<Item> = if (searchOpen) {
    items.distinctBy { it.id }
} else {
    ArrangeCatalog.searchResults(items, selectedGenreId, filters, sort)
}

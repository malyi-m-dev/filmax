package com.filmax.feature.search.common

import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.search.SearchRepository
import com.filmax.core.presentation.BaseScreenModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

private const val SEARCH_DEBOUNCE_MILLIS = 400L
private const val PER_PAGE = 20
private const val RECENT_LIMIT = 8

/**
 * Что показывает чип «Все». `api/v1/items` без параметра `type` не ходит, поэтому «все» —
 * это объединение конкретных типов; ItemType.TV (эфирные каналы) в витрину не входит.
 */
private val BrowseTypes = listOf(ItemType.MOVIE, ItemType.SERIES, ItemType.ANIME, ItemType.DOCUMENTARY)

/**
 * Типы жанров, которые показываем в каталоге. `api/v1/genres` отдаёт одним списком жанры всех
 * разделов kino.pub, включая музыкальные («Blues», «Chillout»), — без этого фильтра они лезли
 * в чипы рядом с «Драмой». Значения совпадают с [ItemType.apiValue] соответствующих типов.
 */
private val VIDEO_GENRE_TYPES = setOf("movie", "serial", "anime", "docuserial", "documovie", "tvshow", "3d")

private val TrendingQueries = listOf(
    "Мстители",
    "Дюна",
    "Офис",
    "Ведьмак",
    "Интерстеллар",
    "Во все тяжкие",
    "Оппенгеймер",
    "Игра престолов",
)

class SearchScreenModel(
    private val search: SearchRepository,
    private val catalog: CatalogRepository,
) : BaseScreenModel<SearchState, SearchSideEffect, SearchEvent>(SearchState()) {

    private val queryFlow = MutableStateFlow("")

    init {
        onFetchData()
    }

    override fun dispatch(event: SearchEvent) {
        when (event) {
            is SearchEvent.QueryChange -> onQueryChange(event.query)
            is SearchEvent.FilterChange -> updateAndReload { it.copy(filter = event.filter) }
            is SearchEvent.SortChange -> updateAndReload { it.copy(sort = event.sort) }
            is SearchEvent.GenreChange -> updateAndReload { it.copy(selectedGenreId = event.genreId) }
            is SearchEvent.ApplyFilters -> updateAndReload { it.copy(filters = event.filters) }
            SearchEvent.ResetFilters -> updateAndReload { it.copy(filters = CatalogFilters()) }
            is SearchEvent.SubmitQuery -> {
                onQueryChange(event.query)
                screenModelScope { _ -> performSearch(event.query) }
            }

            SearchEvent.ClearRecent -> screenModelScope { _ ->
                updateState { it.copy(recentQueries = emptyList()) }
            }

            SearchEvent.LoadCatalog -> onLoadCatalog()
        }
    }

    @OptIn(FlowPreview::class)
    override fun onFetchData() {
        screenModelScope { _ -> updateState { it.copy(trendingQueries = TrendingQueries) } }
        screenModelScope { _ ->
            queryFlow
                // drop(1): StateFlow отдаёт текущее значение сразу при подписке, и без этого
                // стартовый пустой запрос вызвал бы загрузку витрины второй раз — следом за
                // той, что уже запустил LoadCatalog.
                .drop(1)
                .debounce(SEARCH_DEBOUNCE_MILLIS)
                .distinctUntilChanged()
                // collectLatest, а не launch на каждый запрос: на пульте текст набирают по
                // букве, и недосчитанный поиск прошлой подстроки не должен перезаписать
                // выдачу более свежего запроса.
                .collectLatest { reload() }
        }
    }

    private fun onQueryChange(query: String) {
        queryFlow.value = query
        screenModelScope { _ ->
            updateState { it.copy(query = query, error = null) }
            if (query.isBlank()) updateState { it.copy(results = emptyList(), loading = false) }
        }
    }

    /** Смена любого фильтра — это всегда «поправь состояние и перезапроси выдачу». */
    private fun updateAndReload(change: (SearchState) -> SearchState) {
        screenModelScope { _ ->
            updateState(change)
            reload()
        }
    }

    private fun onLoadCatalog() {
        if (state.catalogEnabled) return
        screenModelScope { _ ->
            updateState { it.copy(catalogEnabled = true) }
            reload()
        }
        // Жанры — отдельной корутиной: чипы не должны ждать сетку, а сетка — чипы.
        screenModelScope { _ ->
            catalog.getGenres().getOrNull()
                ?.filter { it.type in VIDEO_GENRE_TYPES }
                ?.let { genres -> updateState { it.copy(genres = genres) } }
        }
        // Страны — тоже отдельно: список нужен только листу фильтров, сетку он не держит.
        screenModelScope { _ ->
            catalog.getCountries().getOrNull()
                ?.let { countries -> updateState { it.copy(countries = countries) } }
        }
    }

    /** Единственная развилка экрана: есть запрос — ищем, нет — показываем витрину по фильтрам. */
    private suspend fun reload() {
        val query = state.query
        if (query.length >= MIN_QUERY_LENGTH) performSearch(query) else loadCatalog()
    }

    private suspend fun performSearch(query: String) {
        updateState { it.copy(loading = true) }
        when (val result = search.search(query, state.filter, perPage = PER_PAGE)) {
            is RequestResult.Success -> updateState { current ->
                val recent = (listOf(query) + current.recentQueries).distinct().take(RECENT_LIMIT)
                current.copy(
                    loading = false,
                    results = arrange(result.data, current),
                    recentQueries = recent,
                )
            }

            is RequestResult.Error -> updateState {
                it.copy(loading = false, error = result.message)
            }
        }
    }

    private suspend fun loadCatalog() {
        if (!state.catalogEnabled) return
        updateState { it.copy(loading = true) }
        val genreId = state.selectedGenreId
        val sort = state.sort
        val filters = state.filters
        val types = state.filter?.let { listOf(it) } ?: BrowseTypes
        val pages = coroutineScope {
            types.map { type -> async { catalog.getItems(type, genreId, filters, sort) } }.awaitAll()
        }
        val lists = pages.mapNotNull { it.getOrNull()?.items }
        if (lists.isEmpty()) {
            val message = pages.firstNotNullOfOrNull { (it as? RequestResult.Error)?.message }
            updateState { it.copy(loading = false, catalogItems = emptyList(), error = message) }
            return
        }
        val merged = sortLocally(interleave(lists), sort)
        updateState { it.copy(loading = false, catalogItems = merged, error = null) }
    }
}

/**
 * Жанр, диапазонные фильтры и сортировка поверх выдачи поиска: сам `search` ничего из этого не
 * принимает. Страну (по id) и 4K локально не проверить — в [Item] их нет, они остаются серверными
 * и на результаты поиска не влияют.
 */
private fun arrange(items: List<Item>, state: SearchState): List<Item> {
    val genreId = state.selectedGenreId
    val filtered = items.asSequence()
        .filter { item -> genreId == null || item.genres.any { it.id == genreId } }
        .filter { item -> item.matches(state.filters) }
        .toList()
    return sortLocally(filtered, state.sort)
}

/** Локальная проверка фильтров по полям, которые есть в [Item] (год, рейтинги, завершённость). */
private fun Item.matches(filters: CatalogFilters): Boolean {
    // Локальные копии: nullable-поля CatalogFilters лежат в другом модуле, и смарт-каст через
    // границу модуля невозможен — сравнивать надо через захваченное значение.
    val yearFrom = filters.yearFrom
    val yearTo = filters.yearTo
    val kpFrom = filters.kpRatingFrom
    val imdbFrom = filters.imdbRatingFrom
    val finishedFilter = filters.onlyFinished
    return (yearFrom == null || year >= yearFrom) &&
        (yearTo == null || year <= yearTo) &&
        (kpFrom == null || ratingAtLeast(rating.kinopoisk, kpFrom)) &&
        (imdbFrom == null || ratingAtLeast(rating.imdb, imdbFrom)) &&
        (finishedFilter == null || finished == finishedFilter)
}

private fun ratingAtLeast(raw: String?, threshold: Int): Boolean {
    val value = raw?.toDoubleOrNull() ?: return false
    return value >= threshold
}

/**
 * Досортировка на клиенте. Набор карточек выбирает сервер, но по «Рейтингу» и «Году» он
 * сортирует по своим полям, а карточка показывает НАШ усреднённый рейтинг (IMDb+КП) и год —
 * без локального прохода первой в сетке стояла бы не та карточка, которую зритель видит лучшей.
 * У остальных ключей (просмотры, новизна, рейтинги КП/IMDb по отдельности) локального поля в
 * [Item] нет: там доверяем порядку сервера как есть, направление он тоже уже применил.
 */
private fun sortLocally(items: List<Item>, sort: SortOption): List<Item> {
    val comparator = when (sort.field) {
        CatalogSort.RATING -> compareBy<Item> { it.rating.external }
        CatalogSort.YEAR -> compareBy<Item> { it.year }
        else -> return items
    }
    return if (sort.ascending) items.sortedWith(comparator) else items.sortedWith(comparator.reversed())
}

/**
 * Склейка выдачи нескольких типов по кругу (для чипа «Все»). Простой `flatten()` дал бы
 * 20 фильмов, потом 20 сериалов — до аниме зритель не долистал бы никогда.
 */
private fun interleave(lists: List<List<Item>>): List<Item> {
    if (lists.size == 1) return lists.first()
    val depth = lists.maxOf { it.size }
    return (0 until depth).flatMap { index -> lists.mapNotNull { it.getOrNull(index) } }
}

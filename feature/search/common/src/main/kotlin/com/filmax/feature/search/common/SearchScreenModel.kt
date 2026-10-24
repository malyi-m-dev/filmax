package com.filmax.feature.search.common

import androidx.lifecycle.SavedStateHandle
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
import com.filmax.feature.search.common.navigation.browseSection
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

private const val SEARCH_DEBOUNCE_MILLIS = 300L
private const val PER_PAGE = 20
private const val RECENT_LIMIT = 8

/**
 * Что показывает чип «Все». `api/v1/items` без параметра `type` не ходит, поэтому «все» —
 * это объединение конкретных типов; ItemType.TV (эфирные каналы) в витрину не входит.
 * ANIME здесь нет: у kino.pub нет такого типа (api/v1/types), и аниме-тайтлы уже входят
 * в выдачу как movie/serial со своим жанром.
 */
private val BrowseTypes = listOf(ItemType.MOVIE, ItemType.SERIES, ItemType.DOCUMENTARY)

/**
 * Чип «Аниме»: типа «anime» у kino.pub НЕТ — аниме это ЖАНР (id 25) поверх фильмов и
 * сериалов. Поэтому фильтр ANIME разворачивается в movie+serial с жанром [ANIME_GENRE_ID];
 * выбранный в ряду жанр на это время игнорируется — параметр `genre` в API один.
 */
private const val ANIME_GENRE_ID = 25
private val AnimeTypes = listOf(ItemType.MOVIE, ItemType.SERIES)

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

/**
 * Поиск и витрина каталога. [savedStateHandle] несёт раздел ([browseSection]), если экран открыт
 * разделом с Главной: выборка раздела — стартовое состояние, а не событие от экрана, поэтому
 * первая же загрузка идёт уже по ней.
 */
class SearchScreenModel(
    private val search: SearchRepository,
    private val catalog: CatalogRepository,
    savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : BaseScreenModel<SearchState, SearchSideEffect, SearchEvent>(
    SearchState().withSection(savedStateHandle.browseSection()),
) {

    private val queryFlow = MutableStateFlow("")
    private val pager = CatalogPager(catalog)

    /**
     * Поиск открыт готовым запросом (голос, недавний): как только найдётся лучшее совпадение,
     * фокус уходит на него. Зритель начал набирать сам — прыгать фокусу уже незачем.
     */
    private var focusMatchWhenFound = false

    init {
        onFetchData()
    }

    override fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.QueryChange -> onQueryChange(event.query)
            is SearchEvent.Selection -> updateAndReload { it.withSelection(event) }
            // Готовый запрос (подсказка, недавний, голос): искать сразу, без debounce, и запомнить.
            is SearchEvent.SubmitQuery -> {
                onQueryChange(event.query)
                screenModelScope { _ ->
                    updateState { it.withRecent(event.query) }
                    performSearch(event.query)
                }
            }

            SearchEvent.ResultOpened -> screenModelScope { _ -> updateState { it.withRecent(it.query) } }
            SearchEvent.ClearRecent -> screenModelScope { _ ->
                updateState { it.copy(recentQueries = emptyList()) }
            }

            SearchEvent.LoadCatalog -> onLoadCatalog()
            SearchEvent.LoadMoreCatalog -> onLoadMoreCatalog()
            SearchEvent.RetryCatalog -> screenModelScope { _ -> loadCatalog() }
            is SearchEvent.Keyboard -> onKeyboard(event)
            // Фильтры в панели правятся черновиком — выдачу перезапрашивает только «Применить».
            SearchEvent.ApplyDraftFilters -> updateAndReload { it.withSheet(SearchEvent.ApplyDraftFilters) }
            is SearchEvent.Sheet -> screenModelScope { _ -> updateState { it.withSheet(event) } }
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

    /**
     * Поиск на ТВ: экранная клавиатура набирает запрос посимвольно. Новый запрос считается от
     * [queryFlow] — он меняется сразу, а состояние экрана догоняет его в корутине, и быстрые
     * нажатия не теряют букву.
     */
    private fun onKeyboard(event: SearchEvent.Keyboard) {
        // Прыжок фокуса на найденное — только для готового запроса; зритель взялся за клавиатуру
        // сам — фокус остаётся у него.
        focusMatchWhenFound = event is SearchEvent.OpenSearch && !event.query.isNullOrBlank()
        when (event) {
            is SearchEvent.OpenSearch -> {
                screenModelScope { _ -> updateState { it.copy(searchOpen = true) } }
                event.query?.takeIf { it.isNotBlank() }?.let { dispatch(SearchEvent.SubmitQuery(it)) }
            }
            // Закрытый поиск возвращает витрину как была: пейджер помнит загруженную выдачу.
            SearchEvent.CloseSearch -> {
                onQueryChange("")
                screenModelScope { _ -> updateState { it.copy(searchOpen = false) } }
            }

            is SearchEvent.TypeKey -> onQueryChange(typedQuery(queryFlow.value, event.key))
            SearchEvent.DeleteLast -> onQueryChange(queryFlow.value.dropLast(1))
            is SearchEvent.SwitchLayout -> screenModelScope { _ ->
                updateState { it.copy(keyboardLayout = event.layout) }
            }
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
        // Страны — тоже отдельно: список нужен только панели фильтров, сетку он не держит.
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
        when (val result = search.search(query, state.searchType(), perPage = PER_PAGE)) {
            is RequestResult.Success -> {
                updateState { current -> current.copy(loading = false, results = current.arranged(result.data)) }
                if (focusMatchWhenFound && state.bestMatch != null) {
                    focusMatchWhenFound = false
                    postSideEffect(SearchSideEffect.FocusBestMatch)
                }
            }

            is RequestResult.Error -> updateState {
                it.copy(loading = false, error = result.message)
            }
        }
    }

    private suspend fun loadCatalog() {
        if (!state.catalogEnabled) return
        val query = state.catalogQuery()
        // Та же выдача уже в сетке (закрыли поиск, стёрли запрос) — её не перезагружаем: сбросились
        // бы прокрутка и догруженные страницы.
        if (query != pager.loaded) {
            updateState { it.copy(loading = true, catalogLoadingMore = false, catalogEndReached = false) }
        }
        when (val first = pager.first(query)) {
            is CatalogPage.Loaded -> updateState {
                // Уникальность по id: это ключ карточки в сетке, повтор роняет её измерение.
                val items = sortLocally(first.items.distinctBy { item -> item.id }, it.sort)
                it.copy(
                    loading = false,
                    catalogItems = items,
                    catalogEndReached = pager.endReached,
                    catalogCount = pager.count(items.size),
                    error = null,
                )
            }

            is CatalogPage.Failed -> updateState {
                it.copy(loading = false, catalogItems = emptyList(), catalogCount = null, error = first.message)
            }

            CatalogPage.Unchanged -> updateState { it.copy(loading = false) }
            // Ответ на прежние чипы: состояние ведёт та загрузка, что запущена позже.
            CatalogPage.Outdated -> Unit
        }
    }

    /**
     * Догрузка следующей страницы витрины. Идемпотентна: во время загрузки, после конца
     * каталога и в режиме поиска повторный вызов игнорируется — UI может дёргать её при
     * каждом подходе скролла к хвосту сетки.
     */
    private fun onLoadMoreCatalog() {
        val current = state
        val busy = current.loading || current.catalogLoadingMore || current.catalogEndReached
        if (!current.catalogEnabled || busy || current.query.length >= MIN_QUERY_LENGTH) return
        screenModelScope { _ ->
            updateState { it.copy(catalogLoadingMore = true) }
            val next = pager.next()
            updateState { loaded ->
                when (next) {
                    is CatalogPage.Loaded -> {
                        val merged = loaded.catalogItems.mergedWith(next.items, loaded.sort)
                        loaded.copy(
                            catalogLoadingMore = false,
                            catalogItems = merged,
                            catalogEndReached = pager.endReached,
                            catalogCount = pager.count(merged.size),
                        )
                    }
                    // Тихий фейл: показанную витрину не рушим, следующий подход к хвосту повторит.
                    // Устаревшая страница просто не нужна — ту выдачу уже сменили.
                    else -> loaded.copy(catalogLoadingMore = false)
                }
            }
        }
    }
}

/** Выборка после события: тип, сортировка (выбор в панели её закрывает), жанр, фильтры. */
private fun SearchState.withSelection(event: SearchEvent.Selection): SearchState = when (event) {
    is SearchEvent.FilterChange -> copy(filter = event.filter)
    is SearchEvent.SortChange -> copy(sort = event.sort, sheetOpen = false)
    is SearchEvent.GenreChange -> copy(selectedGenreId = event.genreId)
    is SearchEvent.ApplyFilters -> copy(filters = event.filters)
    SearchEvent.ResetFilters -> copy(filters = CatalogFilters())
}

/**
 * Боковые панели ТВ. Панель фильтров открывается с черновиком, равным применённым фильтрам: закрытая
 * без «Применить», она не оставляет следов, а «Применить» переносит черновик в выдачу.
 */
private fun SearchState.withSheet(event: SearchEvent.Sheet): SearchState = when (event) {
    is SearchEvent.OpenSheet -> copy(sheetOpen = true, sheet = event.sheet, filtersDraft = filters)
    SearchEvent.CloseSheet -> copy(sheetOpen = false)
    is SearchEvent.EditFilters -> copy(filtersDraft = event.filters)
    SearchEvent.ResetDraftFilters -> copy(filtersDraft = CatalogFilters())
    SearchEvent.ApplyDraftFilters -> copy(filters = filtersDraft, sheetOpen = false)
}

/**
 * Тип для запроса поиска. На ТВ поиск — отдельный режим без чипов каталога: фильтр, которого
 * зритель не видит, тихо прятал бы найденное, поэтому там ищем по всему каталогу.
 */
private fun SearchState.searchType(): ItemType? = if (searchOpen) null else filter

/**
 * Жанр, диапазонные фильтры и сортировка поверх выдачи поиска: сам `search` ничего из этого не
 * принимает. Страну (по id) и 4K локально не проверить — в [Item] их нет, они остаются серверными
 * и на результаты поиска не влияют. На ТВ фильтры к поиску не применяются (см. [searchType]).
 */
private fun SearchState.arranged(items: List<Item>): List<Item> {
    // distinctBy: поиск умеет вернуть один id дважды — в сетке это дубль ключа и краш.
    if (searchOpen) return items.distinctBy { it.id }
    val genreId = selectedGenreId
    val filtered = items.asSequence()
        .filter { item -> genreId == null || item.genres.any { it.id == genreId } }
        .filter { item -> item.matches(filters) }
        .distinctBy { it.id }
        .toList()
    return sortLocally(filtered, sort)
}

/** Выдача витрины по фильтрам экрана; «Аниме» — жанр поверх фильмов и сериалов. */
private fun SearchState.catalogQuery(): CatalogQuery = CatalogQuery(
    types = when (filter) {
        null -> BrowseTypes
        ItemType.ANIME -> AnimeTypes
        else -> listOf(filter)
    },
    genreId = if (filter == ItemType.ANIME) ANIME_GENRE_ID else selectedGenreId,
    filters = filters,
    sort = sort,
)

/** Запрос в начало недавних — если он годится для поиска. */
private fun SearchState.withRecent(query: String): SearchState {
    val trimmed = query.trim()
    if (trimmed.length < MIN_QUERY_LENGTH) return this
    return copy(recentQueries = (listOf(trimmed) + recentQueries).distinct().take(RECENT_LIMIT))
}

/**
 * Догруженная страница в хвост витрины. filter гасит id, уже стоящие в сетке; distinctBy —
 * дубли внутри самой страницы (склейка типов): две карточки с одним id роняют LazyGrid по
 * неуникальному ключу.
 */
private fun List<Item>.mergedWith(page: List<Item>, sort: SortOption): List<Item> {
    val seen = mapTo(HashSet()) { it.id }
    return sortLocally((this + page.filter { it.id !in seen }).distinctBy { it.id }, sort)
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

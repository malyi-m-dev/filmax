package com.filmax.feature.search.common

import androidx.lifecycle.SavedStateHandle
import com.filmax.core.domain.catalog.CatalogGenres
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.search.SearchRepository
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
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

/**
 * Что показывает чип «Все». `api/v1/items` без параметра `type` не ходит, поэтому «все» —
 * это объединение конкретных типов; ItemType.TV (эфирные каналы) в витрину не входит.
 * ANIME здесь нет: у kino.pub нет такого типа (api/v1/types), и аниме-тайтлы уже входят
 * в выдачу как movie/serial со своим жанром.
 */
private val BrowseTypes = listOf(ItemType.MOVIE, ItemType.SERIES, ItemType.DOCUMENTARY)

/**
 * Чип «Аниме»: типа «anime» у kino.pub НЕТ — аниме это ЖАНР (id 25) поверх фильмов и
 * сериалов. Поэтому фильтр ANIME разворачивается в movie+serial с жанром [CatalogGenres.ANIME];
 * выбранный в ряду жанр на это время игнорируется — параметр `genre` в API один.
 */
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
    private val navigator: Navigator,
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
            is SearchEvent.SubmitQuery -> onSubmitQuery(event.query)
            is SearchEvent.Selection -> onSelection(event)
            SearchEvent.ClearRecent -> onClearRecent()
            SearchEvent.LoadCatalog -> onLoadCatalog()
            SearchEvent.LoadMoreCatalog -> onLoadMoreCatalog()
            SearchEvent.RetryCatalog -> onRetryCatalog()
            is SearchEvent.OpenItem -> navigator.open(Destination.Details(event.itemId))
            is SearchEvent.OpenResult -> onOpenResult(event.itemId)
            SearchEvent.Back -> navigator.back()
            is SearchEvent.Keyboard -> onKeyboard(event)
            SearchEvent.ApplyDraftFilters -> onApplyDraftFilters()
            is SearchEvent.Sheet -> onSheet(event)
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

    /**
     * Новый запрос. [queryFlow] меняется сразу, состояние догоняет его в корутине: экранная
     * клавиатура ТВ набирает посимвольно, и быстрые нажатия не теряют букву.
     */
    private fun onQueryChange(query: String) {
        queryFlow.value = query
        screenModelScope { _ -> updateState { it.withQuery(query) } }
    }

    /** Готовый запрос (подсказка, недавний, голос): искать сразу, без debounce, и запомнить. */
    private fun onSubmitQuery(query: String) {
        onQueryChange(query)
        screenModelScope { _ ->
            updateState { it.withRecent(query) }
            performSearch(query)
        }
    }

    /** Смена любого фильтра — это всегда «поправь состояние и перезапроси выдачу». */
    private fun onSelection(event: SearchEvent.Selection) {
        screenModelScope { _ ->
            updateState { it.withSelection(event) }
            reload()
        }
    }

    private fun onClearRecent() {
        screenModelScope { _ -> updateState { it.copy(recentQueries = emptyList()) } }
    }

    /** Открытый результат — повод запомнить запрос: сам набор по букве в недавние не попадает. */
    private fun onOpenResult(itemId: Int) {
        screenModelScope { _ -> updateState { it.withRecent(it.query) } }
        navigator.open(Destination.Details(itemId))
    }

    private fun onKeyboard(event: SearchEvent.Keyboard) {
        // Прыжок фокуса на найденное — только для готового запроса; зритель взялся за клавиатуру
        // сам — фокус остаётся у него.
        focusMatchWhenFound = event is SearchEvent.OpenSearch && !event.query.isNullOrBlank()
        when (event) {
            is SearchEvent.OpenSearch -> onOpenSearch(event.query)
            SearchEvent.CloseSearch -> onCloseSearch()
            is SearchEvent.TypeKey -> onQueryChange(typedQuery(queryFlow.value, event.key))
            SearchEvent.DeleteLast -> onQueryChange(queryFlow.value.dropLast(1))
            is SearchEvent.SwitchLayout -> onSwitchLayout(event.layout)
        }
    }

    private fun onOpenSearch(query: String?) {
        screenModelScope { _ ->
            updateState { it.copy(searchOpen = true) }
            postSideEffect(SearchSideEffect.FocusKeyboard)
        }
        if (!query.isNullOrBlank()) onSubmitQuery(query)
    }

    /** Закрытый поиск возвращает витрину как была: пейджер помнит загруженную выдачу. */
    private fun onCloseSearch() {
        onQueryChange("")
        screenModelScope { _ ->
            updateState { it.copy(searchOpen = false) }
            postSideEffect(SearchSideEffect.FocusSearchField)
        }
    }

    private fun onSwitchLayout(layout: KeyboardLayout) {
        screenModelScope { _ -> updateState { it.copy(keyboardLayout = layout) } }
    }

    private fun onSheet(event: SearchEvent.Sheet) {
        screenModelScope { _ -> updateState { it.withSheet(event) } }
    }

    /** Фильтры в шторке правятся черновиком — выдачу перезапрашивает только «Применить». */
    private fun onApplyDraftFilters() {
        screenModelScope { _ ->
            updateState { it.withSheet(SearchEvent.ApplyDraftFilters) }
            reload()
        }
    }

    private fun onLoadCatalog() {
        screenModelScope { current ->
            if (current.catalogEnabled) return@screenModelScope
            updateState { it.copy(catalogEnabled = true) }
            loadGenres()
            loadCountries()
            reload()
        }
    }

    private fun onRetryCatalog() {
        screenModelScope { _ -> loadCatalog() }
    }

    /** Жанры — отдельной корутиной: чипы не должны ждать сетку, а сетка — чипы. */
    private fun loadGenres() {
        screenModelScope { _ ->
            val genres = catalog.getGenres().getOrNull()?.filter { it.type in VIDEO_GENRE_TYPES }
                ?: return@screenModelScope
            updateState { it.copy(genres = genres) }
        }
    }

    /** Страны — тоже отдельно: список нужен только шторке фильтров, сетку он не держит. */
    private fun loadCountries() {
        screenModelScope { _ ->
            val countries = catalog.getCountries().getOrNull() ?: return@screenModelScope
            updateState { it.copy(countries = countries) }
        }
    }

    /** Единственная развилка экрана: есть запрос — ищем, нет — показываем витрину по фильтрам. */
    private suspend fun reload() {
        val query = state.query
        if (query.length >= MIN_QUERY_LENGTH) performSearch(query) else loadCatalog()
    }

    private suspend fun performSearch(query: String) {
        updateState { it.copy(loading = true) }
        // На ТВ поиск — режим без чипов каталога: фильтр, которого зритель не видит, тихо прятал
        // бы найденное, поэтому там ищем по всему каталогу.
        val type = if (state.searchOpen) null else state.filter
        when (val result = search.search(query, type, perPage = PER_PAGE)) {
            is RequestResult.Success -> {
                val results = arrangedResults(state, result.data)
                updateState { it.withResults(results) }
                focusBestMatchIfWanted()
            }

            is RequestResult.Error -> updateState { it.copy(loading = false, error = result.message) }
        }
    }

    private suspend fun focusBestMatchIfWanted() {
        if (!focusMatchWhenFound || state.bestMatch == null) return
        focusMatchWhenFound = false
        postSideEffect(SearchSideEffect.FocusBestMatch)
    }

    private suspend fun loadCatalog() {
        val current = state
        if (!current.catalogEnabled) return
        val query = catalogQuery(current)
        // Та же выдача уже в сетке (закрыли поиск, стёрли запрос) — её не перезагружаем: сбросились
        // бы прокрутка и догруженные страницы.
        if (query != pager.loaded) {
            updateState { it.copy(loading = true, catalogLoadingMore = false, catalogEndReached = false) }
        }
        when (val first = pager.first(query)) {
            is CatalogPage.Loaded -> {
                // Уникальность по id: это ключ карточки в сетке, повтор роняет её измерение.
                val items = sortedLocally(first.items.distinctBy { it.id }, current.sort)
                val count = pager.count(items.size)
                updateState { it.withCatalog(items, pager.endReached, count) }
            }

            is CatalogPage.Failed -> updateState { it.withCatalogFailed(first.message) }
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
        screenModelScope { current ->
            val busy = current.loading || current.catalogLoadingMore || current.catalogEndReached
            if (!current.catalogEnabled || busy || current.searching) return@screenModelScope
            updateState { it.copy(catalogLoadingMore = true) }
            when (val next = pager.next()) {
                is CatalogPage.Loaded -> {
                    val merged = appendedPage(state.catalogItems, next.items, state.sort)
                    val count = pager.count(merged.size)
                    updateState { it.withMorePage(merged, pager.endReached, count) }
                }
                // Тихий фейл: показанную витрину не рушим, следующий подход к хвосту повторит.
                // Устаревшая страница просто не нужна — ту выдачу уже сменили.
                else -> updateState { it.copy(catalogLoadingMore = false) }
            }
        }
    }

    /** Выдача витрины по фильтрам экрана; «Аниме» — жанр поверх фильмов и сериалов. */
    private fun catalogQuery(current: SearchState): CatalogQuery {
        val filter = current.filter
        return CatalogQuery(
            types = when (filter) {
                null -> BrowseTypes
                ItemType.ANIME -> AnimeTypes
                else -> listOf(filter)
            },
            genreId = if (filter == ItemType.ANIME) CatalogGenres.ANIME else current.selectedGenreId,
            filters = current.filters,
            sort = current.sort,
        )
    }
}

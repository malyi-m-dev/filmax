package com.filmax.feature.search.common

import androidx.lifecycle.SavedStateHandle
import com.filmax.core.domain.catalog.ArrangeCatalog
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.search.SearchRepository
import com.filmax.core.domain.source.ContentSourceRepository
import com.filmax.core.domain.source.capabilities
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
    sources: ContentSourceRepository,
    savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : BaseScreenModel<SearchState, SearchSideEffect, SearchEvent>(
    // Возможности источника — на старте экрана: смена источника пересоздаёт экраны (граф к корню).
    SearchState(capabilities = sources.source.value.capabilities).withSection(savedStateHandle.browseSection()),
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
                screenModelScope {
                    updateState { it.withRecent(event.query) }
                    performSearch(event.query)
                }
            }

            SearchEvent.ResultOpened -> screenModelScope { updateState { it.withRecent(it.query) } }
            SearchEvent.ClearRecent -> screenModelScope {
                updateState { it.copy(recentQueries = emptyList()) }
            }

            SearchEvent.LoadCatalog -> onLoadCatalog()
            SearchEvent.LoadMoreCatalog -> onLoadMoreCatalog()
            SearchEvent.RetryCatalog -> screenModelScope { loadCatalog() }
            is SearchEvent.Keyboard -> onKeyboard(event)
            // Фильтры в панели правятся черновиком — выдачу перезапрашивает только «Применить».
            SearchEvent.ApplyDraftFilters -> updateAndReload { it.withSheet(SearchEvent.ApplyDraftFilters) }
            is SearchEvent.Sheet -> screenModelScope { updateState { it.withSheet(event) } }
        }
    }

    @OptIn(FlowPreview::class)
    override fun onFetchData() {
        screenModelScope { updateState { it.copy(trendingQueries = TrendingQueries) } }
        screenModelScope {
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
        screenModelScope {
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
                screenModelScope { updateState { it.copy(searchOpen = true) } }
                event.query?.takeIf { it.isNotBlank() }?.let { dispatch(SearchEvent.SubmitQuery(it)) }
            }
            // Закрытый поиск возвращает витрину как была: пейджер помнит загруженную выдачу.
            SearchEvent.CloseSearch -> {
                onQueryChange("")
                screenModelScope { updateState { it.copy(searchOpen = false) } }
            }

            is SearchEvent.TypeKey -> onQueryChange(typedQuery(queryFlow.value, event.key))
            SearchEvent.DeleteLast -> onQueryChange(queryFlow.value.dropLast(1))
            is SearchEvent.SwitchLayout -> screenModelScope {
                updateState { it.copy(keyboardLayout = event.layout) }
            }
        }
    }

    /** Смена любого фильтра — это всегда «поправь состояние и перезапроси выдачу». */
    private fun updateAndReload(change: (SearchState) -> SearchState) {
        screenModelScope {
            updateState(change)
            reload()
        }
    }

    private fun onLoadCatalog() {
        if (state.catalogEnabled) return
        screenModelScope {
            updateState { it.copy(catalogEnabled = true) }
            reload()
        }
        // Жанры — отдельной корутиной: чипы не должны ждать сетку, а сетка — чипы.
        screenModelScope {
            catalog.getGenres().getOrNull()
                ?.filter { it.video }
                ?.let { genres -> updateState { it.copy(genres = genres) } }
        }
        // Страны — тоже отдельно: список нужен только панели фильтров, сетку он не держит.
        screenModelScope {
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
                val items = ArrangeCatalog.firstPage(first.items, it.sort)
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
        screenModelScope {
            updateState { it.copy(catalogLoadingMore = true) }
            val next = pager.next()
            updateState { loaded ->
                when (next) {
                    is CatalogPage.Loaded -> {
                        val merged = ArrangeCatalog.nextPage(loaded.catalogItems, next.items, loaded.sort)
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

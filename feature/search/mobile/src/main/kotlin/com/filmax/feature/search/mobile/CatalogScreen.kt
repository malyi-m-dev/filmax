// Каталог: состояние экрана, витрина и режим поиска с их сетками — постеры, загрузка, пустое
// состояние и догрузка страниц. Шапка с сортировкой и чипами — в CatalogHeader, строка поиска
// в обоих режимах — в CatalogSearchBar, лист фильтров — в CatalogFilterSheet.

package com.filmax.feature.search.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxOnSurfaceDim
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.ui.components.FilmaxEmptyState
import com.filmax.core.ui.components.FilmaxPosterCard
import com.filmax.core.ui.components.posterMeta
import com.filmax.core.ui.components.ratingLabel
import com.filmax.core.ui.components.typeLabel
import com.filmax.feature.search.common.MIN_QUERY_LENGTH
import com.filmax.feature.search.common.SearchEvent
import com.filmax.feature.search.common.SearchScreenModel
import com.filmax.feature.search.common.SearchState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** Сетка каталога: 3×98dp постера + 2×12dp зазора + поля 20dp ровно ложатся в кадр 360dp. */
private const val GRID_COLUMNS = 3

/** За сколько хвостовых ячеек сетки до конца просить следующую страницу витрины. */
private const val LOAD_MORE_TAIL = 9

/** Действия каталога одним объектом: у detekt порог LongParameterList — 6. */
internal data class CatalogActions(
    val onOpenItem: (Int) -> Unit,
    val onOpenSearch: () -> Unit,
    val onFilter: (ItemType?) -> Unit,
    val onSort: (SortOption) -> Unit,
    val onGenre: (Int?) -> Unit,
    val onOpenFilters: () -> Unit,
)

internal data class SearchActions(
    val onQueryChange: (String) -> Unit,
    val onSubmitQuery: (String) -> Unit,
    val onOpenItem: (Int) -> Unit,
    val onClose: () -> Unit,
)

/**
 * Каталог (экран «Каталог» макета) — витрина, а не строка поиска: сетка постеров наполнена
 * фильтрами тип/жанр/сортировка ещё до того, как зритель набрал первую букву. Строка поиска
 * здесь — кнопка: набор текста живёт в отдельном режиме [SearchOverlay], где под поле отдана
 * вся шапка, а системная клавиатура открыта сразу.
 *
 * Поверх общего [SearchScreenModel] — тот же debounce-поиск, что и на TV.
 */
@Composable
fun CatalogScreen(
    modifier: Modifier = Modifier,
    screenModel: SearchScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val state by screenModel.collectAsState()
    val onOpenItem: (Int) -> Unit = { itemId ->
        // Открытый результат — повод запомнить запрос в недавних (без запроса модель промолчит).
        screenModel.dispatch(SearchEvent.ResultOpened)
        navigator.open(Destination.Details(itemId))
    }
    var searchMode by remember { mutableStateOf(false) }
    var filtersOpen by remember { mutableStateOf(false) }

    // Витрину и жанры общий ScreenModel тянет только по заявке экрана — без этого сетка пуста.
    LaunchedEffect(Unit) { screenModel.dispatch(SearchEvent.LoadCatalog) }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        if (searchMode) {
            SearchOverlay(
                state = state,
                actions = SearchActions(
                    onQueryChange = { screenModel.dispatch(SearchEvent.QueryChange(it)) },
                    onSubmitQuery = { screenModel.dispatch(SearchEvent.SubmitQuery(it)) },
                    onOpenItem = onOpenItem,
                    onClose = { searchMode = false },
                ),
            )
        } else {
            CatalogContent(
                state = state,
                actions = CatalogActions(
                    onOpenItem = onOpenItem,
                    onOpenSearch = { searchMode = true },
                    onFilter = { screenModel.dispatch(SearchEvent.FilterChange(it)) },
                    onSort = { screenModel.dispatch(SearchEvent.SortChange(it)) },
                    onGenre = { screenModel.dispatch(SearchEvent.GenreChange(it)) },
                    onOpenFilters = { filtersOpen = true },
                ),
                onLoadMore = { screenModel.dispatch(SearchEvent.LoadMoreCatalog) },
            )
        }
    }

    if (filtersOpen) {
        CatalogFilterSheet(
            filters = state.filters,
            countries = state.countries,
            onApply = {
                screenModel.dispatch(SearchEvent.ApplyFilters(it))
                filtersOpen = false
            },
            onReset = {
                screenModel.dispatch(SearchEvent.ResetFilters)
                filtersOpen = false
            },
            onDismiss = { filtersOpen = false },
        )
    }
}

@Composable
private fun CatalogContent(state: SearchState, actions: CatalogActions, onLoadMore: () -> Unit) {
    val gridState = rememberLazyGridState()
    // Догрузка витрины при подходе к хвосту сетки. derivedStateOf пересчитывается без
    // рекомпозиции; повторные вызовы гасит идемпотентность модели.
    val loadMore by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - LOAD_MORE_TAIL
        }
    }
    LaunchedEffect(loadMore) { if (loadMore) onLoadMore() }

    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        state = gridState,
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(
            start = FilmaxMetrics.ScreenPadding,
            end = FilmaxMetrics.ScreenPadding,
            top = 6.dp,
            bottom = FilmaxMetrics.ScreenPadding,
        ),
        horizontalArrangement = Arrangement.spacedBy(FilmaxMetrics.CardGap),
        verticalArrangement = Arrangement.spacedBy(FilmaxMetrics.CardGap),
    ) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
            CatalogHeader(state = state, actions = actions)
        }
        posterItems(
            state = state,
            emptySubtitle = "Измените фильтры или запрос",
            onOpenItem = actions.onOpenItem,
        )
        if (state.catalogLoadingMore) {
            item(key = "loading_more", span = { GridItemSpan(maxLineSpan) }) {
                CatalogLoadingMore()
            }
        }
    }
}

/** Хвостовой индикатор догрузки страницы — невысокий, чтобы не дёргать сетку. */
@Composable
private fun CatalogLoadingMore() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp),
        )
    }
}

/**
 * Режим поиска: та же выдача, но под набор текста отдана вся шапка, а клавиатура — системная
 * (в макете она нарисована лишь как иллюстрация). Пустой запрос показывает витрину каталога:
 * пустой экран с подсказками-заглушками зрителю не нужен.
 */
@Composable
private fun SearchOverlay(state: SearchState, actions: SearchActions) {
    // Пока открыт поиск, «Назад» возвращает в каталог, а не уводит с вкладки.
    BackHandler(onBack = actions.onClose)

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        SearchHeader(query = state.query, actions = actions)
        SearchResults(state = state, onOpenItem = actions.onOpenItem)
    }
}

@Composable
private fun SearchResults(state: SearchState, onOpenItem: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        // Клавиатура перекрывает низ окна — без отступа последний ряд из-под неё не достать.
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(
            start = FilmaxMetrics.ScreenPadding,
            end = FilmaxMetrics.ScreenPadding,
            top = 2.dp,
            bottom = FilmaxMetrics.ScreenPadding,
        ),
        horizontalArrangement = Arrangement.spacedBy(FilmaxMetrics.CardGap),
        verticalArrangement = Arrangement.spacedBy(FilmaxMetrics.CardGap),
    ) {
        if (state.query.length >= MIN_QUERY_LENGTH) {
            item(key = "count", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "Результаты · ${state.results.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = FilmaxOnSurfaceDim,
                )
            }
        }
        posterItems(
            state = state,
            emptySubtitle = "Попробуйте другой запрос",
            onOpenItem = onOpenItem,
        )
    }
}

/** Тело обеих сеток: индикатор, пустое состояние или постеры. */
private fun LazyGridScope.posterItems(
    state: SearchState,
    emptySubtitle: String,
    onOpenItem: (Int) -> Unit,
) {
    val posters = state.visibleItems
    when {
        state.loading && posters.isEmpty() ->
            item(key = "loading", span = { GridItemSpan(maxLineSpan) }) { CatalogLoading() }

        posters.isEmpty() -> item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
            CatalogEmpty(error = state.error, subtitle = emptySubtitle)
        }
    }
    items(posters, key = { it.id }) { item ->
        CatalogPoster(item = item, onClick = { onOpenItem(item.id) })
    }
}

@Composable
private fun CatalogLoading() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

/** Сбой сети — не «ничего не найдено»: пустая сетка по обеим причинам, а причины разные. */
@Composable
private fun CatalogEmpty(error: String?, subtitle: String) {
    FilmaxEmptyState(
        icon = if (error == null) Icons.Filled.SearchOff else Icons.Filled.CloudOff,
        title = if (error == null) "Ничего не найдено" else "Не удалось загрузить",
        subtitle = error ?: subtitle,
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
    )
}

@Composable
private fun CatalogPoster(item: Item, onClick: () -> Unit) {
    FilmaxPosterCard(
        title = item.title,
        posterUrl = item.posters.medium.ifEmpty { item.posters.big },
        onClick = onClick,
        width = FilmaxMetrics.GridPosterWidth,
        height = FilmaxMetrics.GridPosterHeight,
        rating = ratingLabel(item.rating.external),
        meta = posterMeta(typeLabel(item.type), item.year),
    )
}

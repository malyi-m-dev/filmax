// Каталог: состояние экрана, сетка постеров со скелетоном, пустым состоянием и догрузкой страниц.
// Стеклянная шапка с типами и чипами выборки — в CatalogHeader, шторки сортировки, жанра и
// фильтров — в CatalogSheets.

package com.filmax.feature.search.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.glassSource
import com.filmax.core.designsystem.rememberGlassState
import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.CatalogSection
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.presentation.labels.posterMeta
import com.filmax.core.presentation.labels.ratingLabel
import com.filmax.core.presentation.labels.typeLabel
import com.filmax.core.ui.components.FilmaxEmptyState
import com.filmax.core.ui.components.FilmaxPosterCard
import com.filmax.core.ui.components.FilmaxPosterSkeleton
import com.filmax.core.ui.components.LocalNavBarPadding
import com.filmax.core.ui.components.label
import com.filmax.feature.search.common.SearchEvent
import com.filmax.feature.search.common.SearchScreenModel
import com.filmax.feature.search.common.SearchState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** За сколько хвостовых ячеек сетки до конца просить следующую страницу витрины. */
private const val LOAD_MORE_TAIL = 9

/** Скелетонов, пока витрина грузится впервые: четыре ряда заполняют экран телефона. */
private const val SKELETON_COUNT = 12

/** Какая шторка каталога открыта. */
internal enum class CatalogSheet { Sort, Genre, Filters }

/** Действия каталога одним объектом: у detekt порог LongParameterList — 6. */
internal data class CatalogActions(
    val onFilter: (ItemType?) -> Unit,
    val onSort: (SortOption) -> Unit,
    val onGenre: (Int?) -> Unit,
    val onFilters: (CatalogFilters) -> Unit,
    val onOpenSheet: (CatalogSheet) -> Unit,
    val onBack: (() -> Unit)?,
)

/**
 * Каталог — витрина: сетка постеров наполнена выборкой тип/жанр/сортировка/фильтры ещё до
 * первого касания. [section] — раздел с Главной («Фильмы», «4K и HDR»): тогда это тот же экран
 * поверх раздела, с названием раздела и кнопкой «назад», а выборку раздела уже применила модель.
 */
@Composable
fun CatalogScreen(
    modifier: Modifier = Modifier,
    section: CatalogSection? = null,
    screenModel: SearchScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val state by screenModel.collectAsState()
    var sheet by rememberSaveable { mutableStateOf<CatalogSheet?>(null) }
    // Витрину и жанры общий ScreenModel тянет только по заявке экрана — без этого сетка пуста.
    LaunchedEffect(Unit) { screenModel.dispatch(SearchEvent.LoadCatalog) }

    val actions = remember(screenModel, navigator, section) {
        CatalogActions(
            onFilter = { screenModel.dispatch(SearchEvent.FilterChange(it)) },
            onSort = { screenModel.dispatch(SearchEvent.SortChange(it)) },
            onGenre = { screenModel.dispatch(SearchEvent.GenreChange(it)) },
            onFilters = { screenModel.dispatch(SearchEvent.ApplyFilters(it)) },
            onOpenSheet = { sheet = it },
            onBack = if (section != null) navigator::back else null,
        )
    }
    Box(modifier.fillMaxSize().background(FilmaxTheme.palette.background)) {
        CatalogBody(
            state = state,
            title = section?.label ?: "Каталог",
            actions = actions,
            onOpenItem = { navigator.open(Destination.Details(it)) },
            onLoadMore = { screenModel.dispatch(SearchEvent.LoadMoreCatalog) },
            // Раздел открыт поверх вкладки — навигации под ним нет, отступаем только от системной панели.
            bottomPadding = if (section == null) LocalNavBarPadding.current else systemBottomInset(),
        )
    }
    sheet?.let { open ->
        CatalogSheetHost(sheet = open, state = state, actions = actions, onDismiss = { sheet = null })
    }
}

// Сетка и шапка над ней: состояние, выборка, переходы и отступ под навигацию.
@Suppress("LongParameterList")
@Composable
private fun CatalogBody(
    state: SearchState,
    title: String,
    actions: CatalogActions,
    onOpenItem: (Int) -> Unit,
    onLoadMore: () -> Unit,
    bottomPadding: Dp,
) {
    val glass = rememberGlassState()
    val density = LocalDensity.current
    var headerHeight by remember { mutableStateOf(0.dp) }
    Box(Modifier.fillMaxSize()) {
        CatalogGrid(
            state = state,
            onOpenItem = onOpenItem,
            onLoadMore = onLoadMore,
            contentPadding = PaddingValues(
                start = FilmaxMetrics.GridPadding,
                end = FilmaxMetrics.GridPadding,
                top = headerHeight + 12.dp,
                bottom = bottomPadding + FilmaxMetrics.GridPadding,
            ),
            modifier = Modifier.glassSource(glass),
        )
        CatalogHeader(
            title = title,
            state = state,
            actions = actions,
            glass = glass,
            modifier = Modifier.onSizeChanged { headerHeight = with(density) { it.height.toDp() } },
        )
    }
}

@Composable
private fun CatalogGrid(
    state: SearchState,
    onOpenItem: (Int) -> Unit,
    onLoadMore: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
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
        columns = GridCells.Fixed(FilmaxMetrics.POSTER_COLUMNS),
        state = gridState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(FilmaxMetrics.CardGap),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        posterItems(state = state, onOpenItem = onOpenItem)
        if (state.catalogLoadingMore) {
            items(FilmaxMetrics.POSTER_COLUMNS, key = { "more-$it" }) { FilmaxPosterSkeleton() }
        }
    }
}

/** Тело сетки: скелетон первой загрузки, пустое состояние или постеры. */
internal fun LazyGridScope.posterItems(state: SearchState, onOpenItem: (Int) -> Unit) {
    val posters = state.visibleItems
    when {
        state.loading && posters.isEmpty() -> items(SKELETON_COUNT, key = { "skeleton-$it" }) { FilmaxPosterSkeleton() }
        posters.isEmpty() -> item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
            CatalogEmpty(error = state.error)
        }
    }
    items(posters, key = { it.id }) { item ->
        CatalogPoster(item = item, onClick = { onOpenItem(item.id) })
    }
}

/** Сбой сети — не «ничего не найдено»: пустая сетка по обеим причинам, а причины разные. */
@Composable
private fun CatalogEmpty(error: String?) {
    FilmaxEmptyState(
        icon = if (error == null) Icons.Filled.SearchOff else Icons.Filled.CloudOff,
        title = if (error == null) "Ничего не найдено" else "Не удалось загрузить",
        subtitle = error ?: "Измените фильтры",
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
    )
}

@Composable
private fun CatalogPoster(item: Item, onClick: () -> Unit) {
    FilmaxPosterCard(
        title = item.title,
        posterUrl = item.posters.medium.ifEmpty { item.posters.big },
        onClick = onClick,
        rating = ratingLabel(item.rating.external),
        meta = posterMeta(typeLabel(item.type), item.year),
    )
}

@Composable
private fun systemBottomInset(): Dp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

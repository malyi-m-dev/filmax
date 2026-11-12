// Каталог: модель и экран из состояния, сетка постеров со скелетоном, пустым состоянием и догрузкой.
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.glassSource
import com.filmax.core.designsystem.rememberGlassState
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.presentation.cards.TitleCard
import com.filmax.core.ui.components.FilmaxEmptyState
import com.filmax.core.ui.components.FilmaxPosterCard
import com.filmax.core.ui.components.FilmaxPosterSkeleton
import com.filmax.core.ui.components.LocalNavBarPadding
import com.filmax.core.ui.preview.PhonePreviews
import com.filmax.feature.search.common.SearchEvent
import com.filmax.feature.search.common.SearchSamples
import com.filmax.feature.search.common.SearchScreenModel
import com.filmax.feature.search.common.SearchState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** За сколько хвостовых ячеек сетки до конца просить следующую страницу витрины. */
private const val LOAD_MORE_TAIL = 9

/** Скелетонов, пока витрина грузится впервые: четыре ряда заполняют экран телефона. */
private const val SKELETON_COUNT = 12

/**
 * Переходы и события каталога одним объектом. Выборка, шторки и догрузка — события модели как есть
 * ([onEvent]); отдельно — то, что уводит с экрана.
 */
internal data class CatalogActions(
    val onEvent: (SearchEvent) -> Unit,
    val onOpenItem: (Int) -> Unit,
    /** «Назад» из раздела; у вкладки Каталога кнопки нет. */
    val onBack: () -> Unit,
) {
    companion object {
        /** Ничего не делает — для превью. */
        val None = CatalogActions(onEvent = {}, onOpenItem = {}, onBack = {})
    }
}

/**
 * Каталог — витрина: сетка постеров наполнена выборкой тип/жанр/сортировка/фильтры ещё до
 * первого касания. Открытый с Главной раздел («Фильмы», «4K и HDR») модель берёт из маршрута:
 * тогда это тот же экран поверх раздела, с названием раздела и кнопкой «назад».
 */
@Composable
fun CatalogScreen(
    modifier: Modifier = Modifier,
    screenModel: SearchScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val state by screenModel.collectAsState()
    // Витрину и жанры общий ScreenModel тянет только по заявке экрана — без этого сетка пуста.
    LaunchedEffect(Unit) { screenModel.dispatch(SearchEvent.LoadCatalog) }
    val actions = remember(screenModel, navigator) {
        CatalogActions(
            onEvent = screenModel::dispatch,
            onOpenItem = { navigator.open(Destination.Details(it)) },
            onBack = navigator::back,
        )
    }
    CatalogScreen(state = state, actions = actions, modifier = modifier)
}

/** Каталог из состояния — без Koin и навигатора: его рисуют превью. Шторку открывает модель. */
@Composable
internal fun CatalogScreen(state: SearchState, actions: CatalogActions, modifier: Modifier = Modifier) {
    // Раздел открыт поверх вкладки — навигации под ним нет, отступаем только от системной панели.
    val bottomPadding = if (state.section == null) LocalNavBarPadding.current else systemBottomInset()
    Box(modifier.fillMaxSize().background(FilmaxTheme.palette.background)) {
        CatalogBody(state = state, actions = actions, bottomPadding = bottomPadding)
    }
    if (state.sheetOpen) CatalogSheetHost(state = state, onEvent = actions.onEvent)
}

/** Сетка и шапка над ней: высоту шапки замеряем, чтобы первая строка начиналась под стеклом. */
@Composable
private fun CatalogBody(state: SearchState, actions: CatalogActions, bottomPadding: Dp) {
    val glass = rememberGlassState()
    val density = LocalDensity.current
    var headerHeight by remember { mutableStateOf(0.dp) }
    Box(Modifier.fillMaxSize()) {
        CatalogGrid(
            state = state,
            actions = actions,
            contentPadding = PaddingValues(
                start = FilmaxMetrics.GridPadding,
                end = FilmaxMetrics.GridPadding,
                top = headerHeight + 12.dp,
                bottom = bottomPadding + FilmaxMetrics.GridPadding,
            ),
            modifier = Modifier.glassSource(glass),
        )
        CatalogHeader(
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
    actions: CatalogActions,
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
    LaunchedEffect(loadMore) { if (loadMore) actions.onEvent(SearchEvent.LoadMoreCatalog) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(FilmaxMetrics.POSTER_COLUMNS),
        state = gridState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(FilmaxMetrics.CardGap),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        posterItems(state = state, onOpenItem = actions.onOpenItem)
        if (state.catalogLoadingMore) {
            items(FilmaxMetrics.POSTER_COLUMNS, key = { "more-$it" }) { FilmaxPosterSkeleton() }
        }
    }
}

/** Тело сетки: скелетон первой загрузки, пустое состояние или постеры. */
private fun LazyGridScope.posterItems(state: SearchState, onOpenItem: (Int) -> Unit) {
    val posters = state.visibleCards
    when {
        state.loading && posters.isEmpty() -> items(SKELETON_COUNT, key = { "skeleton-$it" }) { FilmaxPosterSkeleton() }
        posters.isEmpty() -> item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
            CatalogEmpty(error = state.error)
        }
    }
    items(posters, key = { it.id }) { card ->
        CatalogPoster(card = card, onClick = { onOpenItem(card.id) })
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
private fun CatalogPoster(card: TitleCard, onClick: () -> Unit) {
    FilmaxPosterCard(
        title = card.title,
        posterUrl = card.posterUrl,
        onClick = onClick,
        rating = card.rating,
        meta = card.typeAndYear,
    )
}

@Composable
private fun systemBottomInset(): Dp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

private class CatalogStatePreviews : PreviewParameterProvider<SearchState> {
    override val values = SearchSamples.catalog.asSequence()
}

@PhonePreviews
@Composable
private fun CatalogScreenPreview(@PreviewParameter(CatalogStatePreviews::class) state: SearchState) {
    FilmaxTheme { CatalogScreen(state = state, actions = CatalogActions.None) }
}

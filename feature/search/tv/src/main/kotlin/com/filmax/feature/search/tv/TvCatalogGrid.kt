// Сетка витрины: шапка (поле, чипы, сводка) первым элементом во всю ширину, постеры по пять,
// скелетоны догрузки и пустые состояния. Прокрутка — с опорной точкой строки в фокусе.
@file:OptIn(ExperimentalFoundationApi::class)

package com.filmax.feature.search.tv

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.tv.designsystem.FxType
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.PivotBringIntoViewSpec
import com.filmax.core.tv.designsystem.ScrollToTopOnNavFocus
import com.filmax.core.tv.designsystem.TvButton
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvPosterCard
import com.filmax.core.tv.designsystem.TvPosterSize
import com.filmax.core.tv.designsystem.TvPosterSkeleton
import com.filmax.core.tv.designsystem.bringIntoViewAsWhole
import com.filmax.core.tv.designsystem.bringIntoViewWithAbove
import com.filmax.core.tv.designsystem.fxEnter
import com.filmax.core.ui.components.isHighRating
import com.filmax.core.ui.components.posterMeta
import com.filmax.core.ui.components.ratingLabel
import com.filmax.core.ui.components.typeLabel
import com.filmax.feature.search.common.SearchEvent
import com.filmax.feature.search.common.SearchState

/** Пять постеров 152dp с шагом 20dp ровно ложатся между полем меню (80dp) и правым полем (40dp). */
internal const val CATALOG_COLUMNS = 5

/** Строка постеров в фокусе встаёт верхом на эту высоту от края экрана. */
private val RowPivot = 110.dp

/** Догрузка — за две строки до конца сетки: страница успевает прийти раньше, чем зритель дойдёт. */
private const val LOAD_MORE_TAIL = CATALOG_COLUMNS * 2

/** Скелетонов на первой загрузке — две строки, первый экран сетки. */
private const val INITIAL_SKELETONS = CATALOG_COLUMNS * 2

private const val HEADER_KEY = "header"

/** Прокрутка сетки и чипы панелей — живут над сеткой, чтобы пережить поиск. */
@Stable
internal class CatalogGridChrome(val gridState: LazyGridState, val sheetOpeners: SheetOpeners)

/**
 * Витрина: шапка и постеры в одной сетке. Строка в фокусе встаёт на опорную точку [RowPivot];
 * первая строка показывается вместе с шапкой — «вверх» со второй строки возвращает экран к началу.
 */
@Composable
internal fun TvCatalogGrid(
    state: SearchState,
    actions: CatalogActions,
    focus: CatalogScreenFocus,
    chrome: CatalogGridChrome,
) {
    val gridState = chrome.gridState
    ScrollToTopOnNavFocus(gridState)
    LoadMoreNearEnd(gridState) { actions.dispatch(SearchEvent.LoadMoreCatalog) }
    val density = LocalDensity.current
    val spec = remember(density) { PivotBringIntoViewSpec(with(density) { RowPivot.toPx() }) }
    var headerHeight by remember { mutableIntStateOf(0) }
    val aboveFirstRow = remember(density) {
        {
            with(density) { (TvMetrics.ContentTop + TvMetrics.GridGap).toPx() } + headerHeight
        }
    }
    CompositionLocalProvider(LocalBringIntoViewSpec provides spec) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(CATALOG_COLUMNS),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = GridPadding,
            horizontalArrangement = Arrangement.spacedBy(TvMetrics.GridGap),
            verticalArrangement = Arrangement.spacedBy(TvMetrics.GridGap),
        ) {
            item(key = HEADER_KEY, span = { GridItemSpan(maxLineSpan) }) {
                CatalogHeader(
                    state = state,
                    actions = actions,
                    focus = focus,
                    sheetOpeners = chrome.sheetOpeners,
                    modifier = Modifier.onSizeChanged { headerHeight = it.height }.bringIntoViewAsWhole(),
                )
            }
            catalogCells(state, actions, focus, aboveFirstRow)
        }
    }
}

private val GridPadding = PaddingValues(
    start = TvMetrics.SectionStart,
    end = TvMetrics.SectionEnd,
    top = TvMetrics.ContentTop,
    bottom = TvMetrics.SafeVertical + TvMetrics.FocusInset,
)

/** Что под шапкой: скелетоны первой загрузки, пустое состояние, ошибка или постеры. */
private fun LazyGridScope.catalogCells(
    state: SearchState,
    actions: CatalogActions,
    focus: CatalogScreenFocus,
    aboveFirstRow: () -> Float,
) {
    when {
        state.loading -> skeletons(INITIAL_SKELETONS)
        state.catalogItems.isEmpty() -> fullWidth("status") { CatalogStatus(state, actions) }
        else -> {
            posters(state.catalogItems, actions, focus, aboveFirstRow)
            if (state.catalogLoadingMore) skeletons(loadMoreSkeletons(state.catalogItems.size))
        }
    }
}

private fun LazyGridScope.posters(
    items: List<Item>,
    actions: CatalogActions,
    focus: CatalogScreenFocus,
    aboveFirstRow: () -> Float,
) {
    itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
        // Первая строка показывается с шапкой, остальные — целиком с подписью, на опорной точке.
        val reveal = if (index < CATALOG_COLUMNS) {
            Modifier.bringIntoViewWithAbove(
                aboveFirstRow
            )
        } else {
            Modifier.bringIntoViewAsWhole()
        }
        TvPosterCard(
            title = item.title,
            meta = posterMeta(typeLabel(item.type), item.year),
            posterUrl = posterUrl(item),
            onClick = { actions.onOpenItem(item.id) },
            modifier = reveal.fxEnter(index % CATALOG_COLUMNS).then(focus.screen.item("grid:${item.id}")),
            rating = ratingLabel(item.rating.kinopoisk),
            ratingHighlighted = isHighRating(item.rating.kinopoisk),
            size = TvPosterSize.Grid,
        )
    }
}

private fun LazyGridScope.skeletons(count: Int) {
    items(count, key = { index -> "skeleton:$index" }) { TvPosterSkeleton(size = TvPosterSize.Grid) }
}

private fun LazyGridScope.fullWidth(key: String, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}

/** Постер для сетки: средний, если есть, — большой на 152dp не нужен. */
internal fun posterUrl(item: Item): String = item.posters.medium.ifEmpty { item.posters.big }

/**
 * Скелетоны догрузки: добить неполную последнюю строку и ещё строка — столько места займёт
 * страница, и сетка не прыгнет, когда вместо заглушек встанут постеры.
 */
internal fun loadMoreSkeletons(
    shown: Int
): Int = (CATALOG_COLUMNS - shown % CATALOG_COLUMNS) % CATALOG_COLUMNS + CATALOG_COLUMNS

/**
 * Пустая витрина: ничего не подошло под фильтры или выдача не загрузилась. Фокусу есть куда
 * встать и без кнопки — чипы шапки прямо над сообщением.
 */
@Composable
private fun CatalogStatus(state: SearchState, actions: CatalogActions) {
    val colors = LocalFxColors.current
    val failed = state.error != null
    Column(
        Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            if (failed) "Не удалось загрузить каталог" else "Ничего не найдено",
            style = FxType.rowTitle,
            color = colors.text,
        )
        Text(
            if (failed) "Проверьте сеть и попробуйте ещё раз" else "Измените фильтры или жанр",
            style = FxType.meta,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        if (failed) {
            TvButton(
                text = "Повторить",
                onClick = { actions.dispatch(SearchEvent.RetryCatalog) },
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}

/**
 * Догрузка витрины: последний видимый элемент — в [LOAD_MORE_TAIL] от конца, просим следующую
 * страницу. derivedStateOf пересчитывается без рекомпозиции и дёргает её только сменой «пора / не
 * пора»; повторные вызовы гасит идемпотентность модели.
 */
@Composable
private fun LoadMoreNearEnd(gridState: LazyGridState, onLoadMore: () -> Unit) {
    val nearEnd by remember(gridState) {
        derivedStateOf {
            val info = gridState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - LOAD_MORE_TAIL
        }
    }
    LaunchedEffect(nearEnd) { if (nearEnd) onLoadMore() }
}

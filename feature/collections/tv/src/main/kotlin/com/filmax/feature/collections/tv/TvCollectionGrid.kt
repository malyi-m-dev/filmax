// Сетка подборки: шесть колонок постеров, строка с фокусом — у опорной точки, догрузка с хвоста,
// индикатор прокрутки справа в свёрнутом виде.
@file:OptIn(ExperimentalFoundationApi::class)

package com.filmax.feature.collections.tv

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.tv.designsystem.FxMotion
import com.filmax.core.tv.designsystem.TvDimGroup
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvPosterCard
import com.filmax.core.tv.designsystem.TvPosterDetails
import com.filmax.core.tv.designsystem.TvPosterSize
import com.filmax.core.tv.designsystem.TvScreenFocus
import com.filmax.core.tv.designsystem.TvScrollBar
import com.filmax.core.tv.designsystem.fxEnter
import com.filmax.core.presentation.labels.isHighRating
import com.filmax.core.presentation.labels.posterMeta
import com.filmax.core.presentation.labels.ratingLabel
import com.filmax.core.presentation.labels.typeLabel

/** Верх первой строки под раскрытой шапкой: постеры заходят на растворённый низ полосы. */
internal val GridTop = 200.dp

/** Опорная точка строки с фокусом под свёрнутой полосой 72dp. */
private val CollapsedPivot = 112.dp

private val ColumnGap = 16.dp
private val RowGap = 22.dp
private val GridEnd = 30.dp
private val ScrollBarEnd = 16.dp
private val ScrollBarTop = 96.dp
private val ScrollBarWidth = 4.dp
private val ScrollBarHeight = 400.dp

/** За сколько карточек до конца сетки просим следующую страницу — две строки. */
private const val LOAD_MORE_TAIL = COLLECTION_COLUMNS * 2

/** Что сетка умеет: открыть тайтл, догрузить хвост, сообщить строку с фокусом. */
internal class CollectionGridActions(
    val onOpenItem: (Int) -> Unit,
    val onLoadMore: () -> Unit,
    val onFocusRow: (Int) -> Unit,
)

/**
 * Сетка постеров. Строка с фокусом встаёт на опорную точку: под раскрытой шапкой — на своё место
 * под ней (первая строка), под свёрнутой — на 112dp. Точка читается из [collapsed] на каждом
 * шаге прокрутки, поэтому смена шапки и прокрутка к строке идут одновременно.
 */
@Composable
internal fun CollectionGrid(
    items: List<Item>,
    collapsed: Boolean,
    focus: TvScreenFocus,
    actions: CollectionGridActions,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val currentCollapsed by rememberUpdatedState(collapsed)
    val density = LocalDensity.current
    val pivotSpec = remember(density) {
        CollectionPivotSpec { with(density) { (if (currentCollapsed) CollapsedPivot else GridTop).toPx() } }
    }
    LoadMoreNearTail(gridState, items.size, actions.onLoadMore)
    Box(modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalBringIntoViewSpec provides pivotSpec) {
            // В сетке только постеры: фокус в ней — всегда на постере, соседи приглушаются.
            TvDimGroup { PosterGrid(items, gridState, focus, actions) }
        }
        GridScrollBar(gridState, visible = collapsed, Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun PosterGrid(
    items: List<Item>,
    gridState: LazyGridState,
    focus: TvScreenFocus,
    actions: CollectionGridActions,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(COLLECTION_COLUMNS),
        state = gridState,
        modifier = Modifier.fillMaxSize().then(focus.containerModifier),
        horizontalArrangement = Arrangement.spacedBy(ColumnGap),
        verticalArrangement = Arrangement.spacedBy(RowGap),
        contentPadding = PaddingValues(
            start = TvMetrics.SafeHorizontal,
            end = GridEnd,
            top = GridTop,
            bottom = TvMetrics.SafeVertical + TvMetrics.FocusInset,
        ),
    ) {
        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
            CollectionPoster(
                item = item,
                onClick = { actions.onOpenItem(item.id) },
                modifier = focus.item(posterKey(item.id))
                    // Сетка поднимается вслед за шапкой: первая колонка — после её надзаголовка.
                    .fxEnter(index % COLLECTION_COLUMNS + GRID_ENTER_AFTER_HEADER)
                    .onFocusChanged { if (it.hasFocus) actions.onFocusRow(gridRow(index)) },
            )
        }
    }
}

/** Сдвиг появления сетки в шагах fxEnter: первые постеры — через ≈100 мс после шапки. */
private const val GRID_ENTER_AFTER_HEADER = 2

/** Ключ постера для памяти фокуса: возврат из деталей — на тот же тайтл. */
internal fun posterKey(itemId: Int): String = "collection:$itemId"

/**
 * Карточка тайтла — общая для всего ТВ-приложения. В подборке без фокуса — чистый постер:
 * название, мета и оценка только у карточки в фокусе, сетка обложек читается картиной.
 */
@Composable
private fun CollectionPoster(item: Item, onClick: () -> Unit, modifier: Modifier) {
    TvPosterCard(
        title = item.title,
        meta = posterMeta(typeLabel(item.type), item.year),
        posterUrl = item.posters.medium.ifEmpty { item.posters.big },
        onClick = onClick,
        rating = ratingLabel(item.rating.kinopoisk),
        ratingHighlighted = isHighRating(item.rating.kinopoisk),
        size = TvPosterSize.Rail,
        details = TvPosterDetails.OnFocus,
        modifier = modifier,
    )
}

/**
 * Догрузка при подходе к хвосту сетки. derivedStateOf пересчитывается без рекомпозиции; повторные
 * вызовы гасит идемпотентность модели.
 */
@Composable
private fun LoadMoreNearTail(gridState: LazyGridState, count: Int, onLoadMore: () -> Unit) {
    val nearTail by remember(gridState) {
        derivedStateOf {
            val info = gridState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - LOAD_MORE_TAIL
        }
    }
    LaunchedEffect(nearTail, count) { if (nearTail) onLoadMore() }
}

/** Индикатор прокрутки справа — только в свёрнутом виде; бегунок двигается без рекомпозиции. */
@Composable
private fun GridScrollBar(gridState: LazyGridState, visible: Boolean, modifier: Modifier) {
    val alpha by animateFloatAsState(
        if (visible) 1f else 0f,
        FxMotion.fade(),
        label = "scrollBar",
    )
    val thumb by remember(gridState) {
        derivedStateOf {
            val info = gridState.layoutInfo
            scrollThumb(gridState.firstVisibleItemIndex, info.visibleItemsInfo.size, info.totalItemsCount)
        }
    }
    TvScrollBar(
        thumbStart = { thumb.start },
        thumbLength = { thumb.length },
        alpha = { alpha },
        modifier = modifier
            .padding(top = ScrollBarTop, end = ScrollBarEnd)
            .size(ScrollBarWidth, ScrollBarHeight),
    )
}

/**
 * Прокрутка к строке с фокусом: строка встаёт краем на [pivotPx] от верха сетки. Точка меняется
 * вместе со шапкой — лямбда читается на каждом шаге анимации прокрутки. У краёв сетки ленивый
 * контейнер сам ограничит прокрутку.
 */
private class CollectionPivotSpec(private val pivotPx: () -> Float) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float =
        offset - pivotPx()
}

// Эффекты прокрутки ленты Главной: приглушение рядов над фокусом, подсветка и затухание у
// верхнего края, индикатор прокрутки справа. Всё анимируемое читается при отрисовке — лента
// при прокрутке не перекомпонуется.
package com.filmax.feature.home.tv

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.filmax.core.tv.designsystem.FxMotion
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.TvScrollBar
import com.filmax.core.tv.designsystem.fxStaticOverlay
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/** Ряды выше ряда в фокусе — вполсилы: взгляд держится на текущем ряду, а не на уехавшем. */
private const val ABOVE_FOCUS_ALPHA = 0.5f

/** Hero гаснет за первые 300dp прокрутки — за то же расстояние проявляются подсветка и затухание. */
internal val HeroFadeDistance = 300.dp

/** Затухание у верхнего края: глухое до 20% полосы — ряд уходит под него, а не обрезается краем. */
private val TopFadeHeight = 84.dp
private const val TOP_FADE_SOLID = 0.2f

/**
 * Подсветка палитры у верхнего края прокрученной ленты (radial-gradient 50% 100% at 80% 0%
 * на полосе 300dp, прозрачная с 75%): эллипс шириной в половину экрана и высотой в полосу.
 */
private const val WASH_CENTER_X = 0.8f
private const val WASH_RADIUS = 0.375f
private const val WASH_FLATTEN = 0.625f

/** Индикатор прокрутки: 4×320dp у правого края, гаснет через 1.5 с после остановки. */
private val ScrollBarWidth = 4.dp
private val ScrollBarHeight = 320.dp
private val ScrollBarTop = 110.dp
private val ScrollBarEnd = 16.dp
private const val SCROLL_BAR_HIDE_MS = 1_500L

/** Какой ряд ленты в фокусе: ряды выше него приглушаются. */
@Stable
internal class FeedFocus {
    var row by mutableIntStateOf(NO_ROW)

    companion object {
        const val NO_ROW = -1
    }
}

/** Ряд ленты с номером [index]: сообщает о фокусе внутри себя и приглушается, когда фокус ниже. */
@Composable
internal fun Modifier.feedRow(index: Int, feedFocus: FeedFocus): Modifier {
    val above by remember(index, feedFocus) { derivedStateOf { feedFocus.row > index } }
    val alpha by animateFloatAsState(if (above) ABOVE_FOCUS_ALPHA else 1f, FxMotion.focus(), label = "rowAbove")
    return trackFeedRow(index, feedFocus).graphicsLayer { this.alpha = alpha }
}

/** Только сообщает о фокусе — hero гаснет сам, параллаксом. */
internal fun Modifier.trackFeedRow(index: Int, feedFocus: FeedFocus): Modifier =
    onFocusChanged { if (it.hasFocus) feedFocus.row = index }

/** Насколько лента ушла от hero: 0 — hero на месте, 1 — hero погас (или ленту листнули дальше). */
@Composable
internal fun rememberScrolledShare(listState: LazyListState): () -> Float {
    val distance = with(LocalDensity.current) { HeroFadeDistance.toPx() }
    return remember(listState, distance) { { listState.scrolledShare(distance) } }
}

private fun LazyListState.scrolledShare(distance: Float): Float =
    if (firstVisibleItemIndex > 0) 1f else (firstVisibleItemScrollOffset / distance).coerceIn(0f, 1f)

/**
 * Подсветка палитры сверху — проявляется, когда hero с его собственной подсветкой уехал.
 * Градиент неподвижен и лежит готовой текстурой; с прокруткой меняется только прозрачность слоя.
 */
@Composable
internal fun BoxScope.ScrolledWash(scrolled: () -> Float) {
    val wash = LocalFxColors.current.wash
    Box(
        Modifier
            .matchParentSize()
            .graphicsLayer { alpha = scrolled() }
            .fxStaticOverlay {
                val center = Offset(size.width * WASH_CENTER_X, 0f)
                val brush = Brush.radialGradient(listOf(wash, Color.Transparent), center, size.width * WASH_RADIUS)
                scale(scaleX = 1f, scaleY = WASH_FLATTEN, pivot = center) { drawRect(brush) }
            },
    )
}

/** Затухание у верхнего края поверх ленты — только когда hero уехал: на hero оно легло бы пятном. */
@Composable
internal fun ScrolledTopFade(scrolled: () -> Float) {
    val background = LocalFxColors.current.background
    val fade = Brush.verticalGradient(TOP_FADE_SOLID to background, 1f to Color.Transparent)
    Box(
        Modifier
            .fillMaxWidth()
            .height(TopFadeHeight)
            .graphicsLayer { alpha = scrolled() }
            .fxStaticOverlay { drawRect(fade) },
    )
}

/**
 * Индикатор прокрутки справа: виден, пока лента едет, и гаснет через 1.5 с после остановки.
 * Бегунок считается при отрисовке — прокрутка перерисовывает полосу, а не перекомпонует её.
 */
@Composable
internal fun BoxScope.FeedScrollBar(listState: LazyListState) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collectLatest { scrolling ->
            if (!scrolling) delay(SCROLL_BAR_HIDE_MS)
            visible = scrolling
        }
    }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, FxMotion.fade(), label = "scrollBar")
    TvScrollBar(
        thumbStart = { listState.scrollThumb().start },
        thumbLength = { listState.scrollThumb().length },
        alpha = { alpha },
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = ScrollBarTop, end = ScrollBarEnd)
            .size(ScrollBarWidth, ScrollBarHeight),
    )
}

private fun LazyListState.scrollThumb(): ScrollThumb {
    val visible = layoutInfo.visibleItemsInfo
    val firstSize = visible.firstOrNull()?.size?.takeIf { it > 0 } ?: return ScrollThumb.Whole
    val position = firstVisibleItemIndex + firstVisibleItemScrollOffset.toFloat() / firstSize
    return scrollThumb(position, visible.size, layoutInfo.totalItemsCount)
}

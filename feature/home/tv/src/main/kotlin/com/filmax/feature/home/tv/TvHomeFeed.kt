// Лента ТВ-Главной: hero с параллаксом, ряды с опорной точкой и финал ленты.
@file:OptIn(ExperimentalFoundationApi::class)

package com.filmax.feature.home.tv

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.filmax.core.tv.designsystem.FxType
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.PivotBringIntoViewSpec
import com.filmax.core.tv.designsystem.ScrollToTopOnNavFocus
import com.filmax.core.tv.designsystem.TvButton
import com.filmax.core.tv.designsystem.TvButtonStyle
import com.filmax.core.tv.designsystem.TvClock
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvScreenFocus
import com.filmax.core.tv.designsystem.bringIntoViewAsWhole
import com.filmax.core.tv.designsystem.rememberTvScreenFocus
import com.filmax.feature.home.common.HomeEvent
import com.filmax.feature.home.common.HomeState

/** Ряд с фокусом встаёт заголовком на эту высоту от верха экрана. */
private val RowPivot = 78.dp

/** Hero уезжает вдвое медленнее ленты. */
private const val HERO_PARALLAX = 0.5f

/** Между рядами ~36dp: 16 — поле фокуса снизу ряда, остальное — отступ ленты. */
private val RowSpacing = TvMetrics.RowGap - TvMetrics.FocusInset

/**
 * Первый ряд встаёт заголовком на нижний край hero, кадр hero уходит под него — ряд
 * «Продолжить» виден с первого экрана целиком, с подписями.
 */
private val FirstRailTop = TvMetrics.HeroHeight

/** Финал ленты — тише рядов: это подпись, а не контент. */
private const val FEED_END_TEXT_ALPHA = 0.55f

/**
 * Лента Главной — LazyColumn: hero, ряды, финал. Ряд с фокусом встаёт на опорную точку
 * [RowPivot], hero при фокусе — к самому верху ([PivotBringIntoViewSpec]); фокус экрана
 * помнит, с какой карточки ушли в детали ([rememberTvScreenFocus]). Когда hero уехал, сверху
 * проявляются подсветка и затухание, ряды над фокусом приглушены, справа — индикатор
 * прокрутки. Часы — вне скролла.
 */
@Composable
internal fun TvHomeFeed(state: HomeState, offline: Boolean, onEvent: (HomeEvent) -> Unit) {
    val listState = rememberLazyListState()
    ScrollToTopOnNavFocus(listState)
    val focus = rememberTvScreenFocus()
    // Номер ряда в фокусе — только для приглушения рядов над ним; модели про это знать незачем.
    val focusedRow = remember { mutableIntStateOf(NO_ROW) }
    val scrolled = rememberScrolledShare(listState)
    val pivot = with(LocalDensity.current) { RowPivot.toPx() }
    val spec = remember(pivot) { PivotBringIntoViewSpec(pivot) }
    Box(Modifier.fillMaxSize()) {
        ScrolledWash(scrolled)
        CompositionLocalProvider(LocalBringIntoViewSpec provides spec) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().then(focus.containerModifier),
                contentPadding = PaddingValues(bottom = TvMetrics.SafeVertical),
                verticalArrangement = Arrangement.spacedBy(RowSpacing),
            ) {
                // Офлайн-деградация (issue #42): кэшированный контент + баннер «нет сети» вместо ошибки.
                if (offline) item(key = "offline") { OfflineBanner(onReload = { onEvent(HomeEvent.Load) }) }
                heroItem(state, listState, onEvent, focus, focusedRow)
                homeRails(state.rows, onEvent, focus, focusedRow)
                item(key = "end") {
                    FeedEnd(
                        onOpenCatalog = { onEvent(HomeEvent.OpenCatalog) },
                        modifier = Modifier.feedRow(state.rows.size + 1, focusedRow),
                    )
                }
            }
        }
        ScrolledTopFade(scrolled)
        FeedScrollBar(listState)
        TvClock(Modifier.align(Alignment.TopEnd).padding(top = 22.dp, end = TvMetrics.SectionEnd))
    }
}

/** Номер hero среди рядов ленты: ряды считаются с 1. */
private const val HERO_ROW = 0

private fun LazyListScope.heroItem(
    state: HomeState,
    listState: LazyListState,
    onEvent: (HomeEvent) -> Unit,
    focus: TvScreenFocus,
    focusedRow: MutableIntState,
) {
    if (state.hero == null) return
    item(key = "hero") {
        TvHomeHero(
            state,
            onEvent,
            focus,
            Modifier
                .heroFootprint(FirstRailTop - RowSpacing)
                .trackFeedRow(HERO_ROW, focusedRow)
                .heroParallax(listState)
                .bringIntoViewAsWhole(),
        )
    }
}

/**
 * Hero занимает в ленте [height] — меньше, чем рисует: низ кадра уходит под первый ряд, как в
 * макете. Ленивый список рисует следующие элементы поверх предыдущих, поэтому ряд лежит на кадре.
 */
private fun Modifier.heroFootprint(height: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, minOf(height.roundToPx(), placeable.height)) { placeable.place(0, 0) }
}

/**
 * Пока hero первый в ленте, он уезжает с параллаксом и гаснет — кадр не обрывается краем.
 *
 * Сдвиг — только при отрисовке, а не `translationY` слоя: поиск фокуса берёт границы с учётом
 * слоя, и сдвинутые вниз кнопки hero оказывались для него ниже верха первого ряда — «вверх» с
 * ряда не находил hero вовсе. Для фокуса hero стоит там, где он в ленте, — над рядами.
 */
@Composable
private fun Modifier.heroParallax(listState: LazyListState): Modifier {
    val fadeDistance = with(LocalDensity.current) { HeroFadeDistance.toPx() }
    val scrolled by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset else Int.MAX_VALUE
        }
    }
    return graphicsLayer { alpha = (1f - scrolled / fadeDistance).coerceIn(0f, 1f) }
        .drawWithContent {
            // За пределами затухания hero не виден, и сдвигать его незачем (Int.MAX_VALUE — уехал).
            if (scrolled >= fadeDistance) return@drawWithContent
            translate(top = scrolled * HERO_PARALLAX) { this@drawWithContent.drawContent() }
        }
}

/** Баннер «нет сети» над кэшированным контентом; фокус+OK — повторить (issue #42). */
@Composable
private fun OfflineBanner(onReload: () -> Unit) {
    TvButton(
        text = "Нет сети — показаны сохранённые данные. Нажмите, чтобы повторить",
        onClick = onReload,
        style = TvButtonStyle.Secondary,
        leadingIcon = Icons.Rounded.CloudOff,
        modifier = Modifier.padding(start = TvMetrics.SectionStart, top = TvMetrics.ContentTop),
    )
}

/** Финал ленты: дальше — каталог. Без него лента обрывается, и непонятно, всё ли это. */
@Composable
private fun FeedEnd(onOpenCatalog: () -> Unit, modifier: Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = TvMetrics.SectionStart, end = TvMetrics.SectionEnd, top = 12.dp)
            .topDivider()
            .padding(top = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "Это всё на сегодня — остальное в каталоге",
            style = FxType.metaRow,
            color = LocalFxColors.current.text.copy(alpha = FEED_END_TEXT_ALPHA),
        )
        TvButton("Открыть каталог", onClick = onOpenCatalog, style = TvButtonStyle.Secondary)
    }
}

/** Тонкая линия по верхнему краю — отделяет финал от последнего ряда. */
@Composable
private fun Modifier.topDivider(): Modifier {
    val color = LocalFxColors.current.outline
    return drawBehind { drawLine(color, Offset.Zero, Offset(size.width, 0f), strokeWidth = 1.dp.toPx()) }
}

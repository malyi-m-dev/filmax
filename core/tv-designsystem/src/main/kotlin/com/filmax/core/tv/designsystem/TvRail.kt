@file:OptIn(ExperimentalFoundationApi::class)

package com.filmax.core.tv.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text

/** Фокус внутри ряда: карточки ряда приглушают соседей сфокусированной. */
internal val LocalRailFocused = compositionLocalOf { false }

/**
 * Горизонтальный ряд карточек с заголовком — как ряды Google TV:
 *
 * - карточка в фокусе встаёт к левому полю [contentStart], двигается лента, а не фокус;
 * - пока фокус в ряду, соседи сфокусированной приглушены ([TvMetrics.DimmedAlpha]);
 * - ряд — группа фокуса с памятью ([tvFocusGroup]): возврат в ряд — на ту карточку, с которой ушли.
 *
 * [count] — число рядом с заголовком («Продолжить просмотр 4»), [subtitle] — пояснение
 * («на этой неделе»). [contentStart] — 80dp у разделов с меню, 58dp у деталей и подборки.
 */
// Компонент дизайн-системы: параметры — его публичный API (заголовок и хвост опций с дефолтами).
@Suppress("LongParameterList")
@Composable
fun TvRail(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    subtitle: String? = null,
    contentStart: Dp = TvMetrics.SectionStart,
    state: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val pivot = with(LocalDensity.current) { contentStart.toPx() }
    val pivotSpec = remember(pivot) { PivotBringIntoViewSpec(pivot) }
    Column(modifier) {
        RailTitle(title, count, subtitle, Modifier.padding(start = contentStart, bottom = 14.dp))
        CompositionLocalProvider(
            LocalRailFocused provides focused,
            LocalBringIntoViewSpec provides pivotSpec,
        ) {
            LazyRow(
                state = state,
                modifier = Modifier
                    .onFocusChanged { focused = it.hasFocus }
                    .tvFocusGroup(),
                contentPadding = PaddingValues(
                    start = contentStart,
                    end = TvMetrics.SectionEnd,
                    top = TvMetrics.FocusInset,
                    bottom = TvMetrics.FocusInset,
                ),
                horizontalArrangement = Arrangement.spacedBy(TvMetrics.CardGap),
                content = content,
            )
        }
    }
}

@Composable
private fun RailTitle(title: String, count: Int?, subtitle: String?, modifier: Modifier) {
    val colors = LocalFxColors.current
    Row(modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = FxType.rowTitle, color = colors.text)
        val hint = subtitle ?: count?.toString()
        if (hint != null) Text(hint, style = FxType.meta, color = colors.textDim)
    }
}

/** Приглушение соседей карточки в фокусе: только пока фокус внутри ряда ([TvRail]). */
@Composable
internal fun Modifier.railDim(focused: Boolean): Modifier {
    val railFocused = LocalRailFocused.current
    val alpha by animateFloatAsState(
        targetValue = if (railFocused && !focused) TvMetrics.DimmedAlpha else 1f,
        animationSpec = FxMotion.focus(),
        label = "railDim",
    )
    return graphicsLayer { this.alpha = alpha }
}

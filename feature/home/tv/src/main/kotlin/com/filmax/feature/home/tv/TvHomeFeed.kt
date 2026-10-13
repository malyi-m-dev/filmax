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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.filmax.core.tv.designsystem.FxType
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.PivotBringIntoViewSpec
import com.filmax.core.tv.designsystem.ScrollToTopOnNavFocus
import com.filmax.core.tv.designsystem.TvButton
import com.filmax.core.tv.designsystem.TvClock
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.bringIntoViewAsWhole
import com.filmax.core.tv.designsystem.rememberTvScreenFocus
import com.filmax.feature.home.common.HomeState

/** Ряд с фокусом встаёт заголовком на эту высоту от верха экрана. */
private val RowPivot = 78.dp

/** Hero уезжает вдвое медленнее ленты и гаснет на первых 300dp прокрутки. */
private const val HERO_PARALLAX = 0.5f
private val HeroFadeDistance = 300.dp

/** Между рядами ~36dp: 16 — поле фокуса снизу ряда, остальное — отступ ленты. */
private val RowSpacing = TvMetrics.RowGap - TvMetrics.FocusInset

/**
 * Лента Главной — LazyColumn: hero, ряды, финал. Ряд с фокусом встаёт на опорную точку
 * [RowPivot], hero при фокусе — к самому верху ([PivotBringIntoViewSpec]); фокус экрана
 * помнит, с какой карточки ушли в детали ([rememberTvScreenFocus]). Часы — вне скролла.
 */
@Composable
internal fun TvHomeFeed(state: HomeState, offline: Boolean, actions: TvHomeActions) {
    val listState = rememberLazyListState()
    ScrollToTopOnNavFocus(listState)
    val focus = rememberTvScreenFocus()
    val pivot = with(LocalDensity.current) { RowPivot.toPx() }
    val spec = remember(pivot) { PivotBringIntoViewSpec(pivot) }
    val hero = HeroModel(
        heroes = state.heroes,
        index = state.heroIndex,
        inWatchLater = state.hero?.id in state.watchLaterIds,
    )

    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalBringIntoViewSpec provides spec) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().then(focus.containerModifier),
                contentPadding = PaddingValues(bottom = TvMetrics.SafeVertical),
                verticalArrangement = Arrangement.spacedBy(RowSpacing),
            ) {
                // Офлайн-деградация (issue #42): кэшированный контент + баннер «нет сети» вместо ошибки.
                if (offline) item(key = "offline") { OfflineBanner(actions.onReload) }
                if (hero.current != null) {
                    item(key = "hero") {
                        TvHomeHero(hero, actions.hero, focus, Modifier.heroParallax(listState).bringIntoViewAsWhole())
                    }
                }
                homeRails(state.rows, actions.rails, focus)
                item(key = "end") { FeedEnd(actions.onOpenCatalog) }
            }
        }
        TvClock(Modifier.align(Alignment.TopEnd).padding(top = 22.dp, end = TvMetrics.SectionEnd))
    }
}

/** Пока hero первый в ленте, он уезжает с параллаксом и гаснет — кадр не обрывается краем. */
@Composable
private fun Modifier.heroParallax(listState: LazyListState): Modifier {
    val fadeDistance = with(LocalDensity.current) { HeroFadeDistance.toPx() }
    val scrolled by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset else Int.MAX_VALUE
        }
    }
    return graphicsLayer {
        translationY = scrolled * HERO_PARALLAX
        alpha = (1f - scrolled / fadeDistance).coerceIn(0f, 1f)
    }
}

/** Баннер «нет сети» над кэшированным контентом; фокус+OK — повторить (issue #42). */
@Composable
private fun OfflineBanner(onReload: () -> Unit) {
    TvButton(
        text = "Нет сети — показаны сохранённые данные. Нажмите, чтобы повторить",
        onClick = onReload,
        primary = false,
        leadingIcon = Icons.Rounded.CloudOff,
        modifier = Modifier.padding(start = TvMetrics.SectionStart, top = TvMetrics.ContentTop),
    )
}

/** Финал ленты: дальше — каталог. Без него лента обрывается, и непонятно, всё ли это. */
@Composable
private fun FeedEnd(onOpenCatalog: () -> Unit) {
    Row(
        Modifier
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
            color = LocalFxColors.current.textSecondary
        )
        TvButton("Открыть каталог", onClick = onOpenCatalog, primary = false)
    }
}

/** Тонкая линия по верхнему краю — отделяет финал от последнего ряда. */
@Composable
private fun Modifier.topDivider(): Modifier {
    val color = LocalFxColors.current.outline
    return drawBehind { drawLine(color, Offset.Zero, Offset(size.width, 0f), strokeWidth = 1.dp.toPx()) }
}

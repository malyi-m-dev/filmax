// Панель «Эпизоды» в деталях сериала: сезоны чипами и серии списком. Прокручивается только она.
@file:OptIn(ExperimentalFoundationApi::class)

package com.filmax.feature.details.tv

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.tv.designsystem.FxMotion
import com.filmax.core.tv.designsystem.FxType
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.LocalReducedMotion
import com.filmax.core.tv.designsystem.PivotBringIntoViewSpec
import com.filmax.core.tv.designsystem.TvChip
import com.filmax.core.tv.designsystem.TvChipStyle
import com.filmax.core.tv.designsystem.TvEpisodeRow
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvScreenFocus
import com.filmax.core.tv.designsystem.rememberTvScrollFades
import com.filmax.core.tv.designsystem.tvFocusGroup
import com.filmax.core.tv.designsystem.tvScrollFades
import com.filmax.feature.details.common.DetailsState
import com.filmax.feature.details.common.startEpisodeIndex

/** Серия в фокусе держится третьей сверху: видно и что было, и что дальше. */
private val EpisodePivot = 162.dp

/** Панель отстоит от краёв экрана справа, сверху и снизу. */
private val PanelMargin = 24.dp
private val PanelPaddingTop = 18.dp
private val PanelPadding = 16.dp

/** Заголовок панели чуть глубже чипов и строк — в линию с текстом строк, а не с их кадрами. */
private val HeaderInset = 4.dp

/**
 * Тёмное стекло: фон экрана на 72% и тонкая светлая обводка. Светлое стекло на ярком кадре
 * сериала теряло текст серий — тёмное держит контраст на любом фоне.
 */
private const val GLASS_ALPHA = 0.72f
private const val GLASS_OUTLINE_ALPHA = 0.1f

/** Последняя серия проскролливается выше нижнего затухания. */
private val ListBottomPadding = 40.dp

/** Сколько серий выше стартовой оставить видимыми при открытии сезона. */
private const val ROWS_ABOVE_START = 2

/** Смена сезона: новые серии поднимаются на 12dp и проявляются, старые гаснут быстрее. */
private val SeasonSlide = 12.dp
private const val SEASON_EXIT_MS = 120

/**
 * Стеклянная панель «Эпизоды»: шапка с числом серий, сезоны компактными чипами и серии
 * списком. Сезон выбирается фокусом на чипе — модель выжидает 250 мс, прежде чем его сменить.
 * [exitLeft] — куда уводит «←» из панели (главная кнопка, а не описание). Задаётся на каждой
 * строке серий и на первом чипе сезонов: направленные свойства Compose берёт у узла под фокусом,
 * а не у контейнера.
 */
@Composable
internal fun TvEpisodesPanel(
    state: DetailsState,
    actions: DetailsActions,
    focus: TvScreenFocus,
    exitLeft: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val glass = LocalFxColors.current.background.copy(alpha = GLASS_ALPHA)
    Column(
        modifier
            .width(TvMetrics.EpisodesPanelWidth + PanelMargin)
            .padding(top = PanelMargin, end = PanelMargin, bottom = PanelMargin)
            .background(glass, TvMetrics.SheetShape)
            .border(1.dp, Color.White.copy(alpha = GLASS_OUTLINE_ALPHA), TvMetrics.SheetShape)
            .padding(top = PanelPaddingTop),
    ) {
        val exit = Modifier.focusProperties { left = exitLeft }
        PanelHeader(state)
        SeasonChips(state, actions, exit)
        SeasonEpisodes(state, actions, focus, exit, Modifier.weight(1f))
    }
}

@Composable
private fun PanelHeader(state: DetailsState) {
    val colors = LocalFxColors.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = PanelPadding + HeaderInset),
        verticalAlignment = Alignment.Bottom
    ) {
        Text("Эпизоды", style = FxType.rowTitle, color = colors.text, modifier = Modifier.weight(1f))
        seasonSummary(state.seasonEpisodes)?.let { Text(it, style = FxType.metaOnImage, color = colors.textDim) }
    }
}

@Composable
private fun SeasonChips(state: DetailsState, actions: DetailsActions, exit: Modifier) {
    val seasons = state.series?.seasons.orEmpty()
    if (seasons.size < 2) return
    // Вход в ряд из серий — на выбранный сезон: ближайший чип переключил бы сезон фокусом.
    val selectedChip = remember { FocusRequester() }
    val firstSeason = seasons.first().first
    LazyRow(
        modifier = Modifier.tvFocusGroup(fallback = selectedChip),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = PanelPadding),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(seasons, key = { it.first }) { (number, _) ->
            val selected = number == state.selectedSeason
            TvChip(
                label = seasonChipLabel(number, selected),
                selected = selected,
                onClick = { actions.onSelectSeason(number) },
                style = TvChipStyle.Compact,
                // Сезон меняется фокусом, без «ОК»: модель гасит быстрое пролистывание задержкой.
                // «←» уводит на кнопки только с первого чипа — с остальных листает сезоны назад.
                modifier = Modifier
                    .then(if (number == firstSeason) exit else Modifier)
                    .then(if (selected) Modifier.focusRequester(selectedChip) else Modifier)
                    .onFocusChanged { if (it.isFocused) actions.onSelectSeason(number) },
            )
        }
    }
}

/**
 * Серии выбранного сезона. Сезон сменился — старый список гаснет, новый поднимается и
 * открывается на серии «продолжить» или первой недосмотренной. Фокус в это время на чипе
 * сезона, поэтому смена списка его не уносит.
 */
@Composable
private fun SeasonEpisodes(
    state: DetailsState,
    actions: DetailsActions,
    focus: TvScreenFocus,
    exit: Modifier,
    modifier: Modifier,
) {
    val slide = with(LocalDensity.current) { SeasonSlide.roundToPx() }
    val reduced = LocalReducedMotion.current
    AnimatedContent(
        targetState = state.selectedSeason,
        transitionSpec = { seasonTransition(slide, reduced) },
        modifier = modifier,
        label = "season",
    ) { season ->
        val episodes = state.series?.seasons?.firstOrNull { it.first == season }?.second.orEmpty()
        EpisodeList(episodes, state.series?.resume?.id, EpisodeListActions(actions, focus, exit))
    }
}

private fun AnimatedContentTransitionScope<Int?>.seasonTransition(slide: Int, reduced: Boolean): ContentTransform =
    if (reduced) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        (fadeIn(FxMotion.fade()) + slideInVertically(FxMotion.fade()) { slide }) togetherWith
            fadeOut(tween(SEASON_EXIT_MS))
    }

/** Что списку серий нужно от экрана: запуск серии, память фокуса и выход «←» на кнопки. */
private class EpisodeListActions(val actions: DetailsActions, val focus: TvScreenFocus, val exit: Modifier)

/**
 * Список серий сезона, открытый на серии «продолжить» или первой недосмотренной. Края списка
 * затухают там, где за ними есть ещё серии ([tvScrollFades]); последняя серия проскролливается
 * выше нижнего затухания.
 */
@Composable
private fun EpisodeList(episodes: List<MediaTrack>, resumeId: Int?, list: EpisodeListActions) {
    val pivot = with(LocalDensity.current) { EpisodePivot.toPx() }
    val spec = remember(pivot) { PivotBringIntoViewSpec(pivot) }
    val start = startEpisodeIndex(episodes, resumeId)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (start - ROWS_ABOVE_START).coerceAtLeast(0))
    val fades = rememberTvScrollFades(listState)
    CompositionLocalProvider(LocalBringIntoViewSpec provides spec) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().tvScrollFades(fades).tvFocusGroup(),
            contentPadding = PaddingValues(
                start = PanelPadding,
                end = PanelPadding,
                top = 2.dp,
                bottom = ListBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(episodes, key = { it.id }) { episode ->
                TvEpisodeRow(
                    model = episodeRow(episode, resumeId),
                    // Плееру нужны номер серии (API `video`) и сезон, а не id трека.
                    onClick = { list.actions.onPlay(episode.seasonNumber, episode.number) },
                    modifier = list.focus.item(episodeKey(episode)).then(list.exit),
                )
            }
        }
    }
}

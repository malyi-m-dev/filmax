// Панель «Эпизоды» в деталях сериала: сезоны чипами и серии списком. Прокручивается только она.
@file:OptIn(ExperimentalFoundationApi::class)

package com.filmax.feature.details.tv

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.filmax.core.tv.designsystem.FxType
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.PivotBringIntoViewSpec
import com.filmax.core.tv.designsystem.TvChip
import com.filmax.core.tv.designsystem.TvChipStyle
import com.filmax.core.tv.designsystem.TvEpisodeRow
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvScreenFocus
import com.filmax.core.tv.designsystem.fxGlass
import com.filmax.core.tv.designsystem.rememberTvScrollFades
import com.filmax.core.tv.designsystem.tvFocusGroup
import com.filmax.core.tv.designsystem.tvScrollFades
import com.filmax.feature.details.common.DetailsState
import com.filmax.feature.details.common.startEpisodeIndex

/** Серия в фокусе держится третьей сверху: видно и что было, и что дальше. */
private val EpisodePivot = 162.dp
private val PanelMargin = 10.dp
private val PanelPadding = 14.dp

/** Последняя серия проскролливается выше нижнего затухания. */
private val ListBottomPadding = 40.dp

/** Сколько серий выше стартовой оставить видимыми при открытии сезона. */
private const val ROWS_ABOVE_START = 2

/**
 * Стеклянная панель «Эпизоды»: шапка с числом серий, сезоны компактными чипами и серии
 * списком. Сезон выбирается фокусом на чипе — модель выжидает 250 мс, прежде чем его сменить.
 * [exitLeft] — куда уводит «←» из панели (главная кнопка, а не описание). Задаётся на каждом
 * чипе и строке: направленные свойства Compose берёт у узла под фокусом, а не у контейнера.
 */
@Composable
internal fun TvEpisodesPanel(
    state: DetailsState,
    actions: DetailsActions,
    focus: TvScreenFocus,
    exitLeft: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .padding(PanelMargin)
            .fxGlass(TvMetrics.SheetShape)
            .padding(top = 16.dp),
    ) {
        val exit = Modifier.focusProperties { left = exitLeft }
        PanelHeader(state)
        SeasonChips(state, actions, exit)
        EpisodeList(state, actions, focus, exit)
    }
}

@Composable
private fun PanelHeader(state: DetailsState) {
    val colors = LocalFxColors.current
    Row(Modifier.fillMaxWidth().padding(horizontal = PanelPadding + 4.dp), verticalAlignment = Alignment.Bottom) {
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
    LazyRow(
        modifier = Modifier.tvFocusGroup(fallback = selectedChip),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = PanelPadding + 4.dp),
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
                modifier = exit
                    .then(if (selected) Modifier.focusRequester(selectedChip) else Modifier)
                    .onFocusChanged { if (it.isFocused) actions.onSelectSeason(number) },
            )
        }
    }
}

/**
 * Серии выбранного сезона. Сезон сменился — список создаётся заново и открывается на серии
 * «продолжить» или первой недосмотренной. Края списка затухают там, где за ними есть ещё
 * серии ([tvScrollFades]); последняя серия проскролливается выше нижнего затухания.
 */
@Composable
private fun ColumnScope.EpisodeList(
    state: DetailsState,
    actions: DetailsActions,
    focus: TvScreenFocus,
    exit: Modifier
) {
    val episodes = state.seasonEpisodes
    val pivot = with(LocalDensity.current) { EpisodePivot.toPx() }
    val spec = remember(pivot) { PivotBringIntoViewSpec(pivot) }
    key(state.selectedSeason) {
        val start = startEpisodeIndex(episodes, state.series?.resume?.id)
        val listState = rememberLazyListState(
            initialFirstVisibleItemIndex = (start - ROWS_ABOVE_START).coerceAtLeast(0)
        )
        val fades = rememberTvScrollFades(listState)
        CompositionLocalProvider(LocalBringIntoViewSpec provides spec) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().tvScrollFades(fades).tvFocusGroup(),
                contentPadding = PaddingValues(
                    start = PanelPadding,
                    end = PanelPadding,
                    top = 4.dp,
                    bottom = ListBottomPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(state.seasonEpisodes, key = { it.id }) { episode ->
                    TvEpisodeRow(
                        model = episodeRow(episode, state.series?.resume?.id),
                        // Плееру нужны номер серии (API `video`) и сезон, а не id трека.
                        onClick = { actions.onPlay(episode.seasonNumber, episode.number) },
                        modifier = focus.item(episodeKey(episode)).then(exit),
                    )
                }
            }
        }
    }
}

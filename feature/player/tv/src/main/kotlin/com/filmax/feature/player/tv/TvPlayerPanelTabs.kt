// Содержимое вкладок нижней панели: серии по сезонам, варианты дорожек и качества сеткой в две
// колонки, плитки скорости. Выбор применяется сразу, панель остаётся открытой.
package com.filmax.feature.player.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.catalog.model.ref
import com.filmax.core.tv.designsystem.FxType
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.TvCardSize
import com.filmax.core.tv.designsystem.TvChip
import com.filmax.core.tv.designsystem.TvChipStyle
import com.filmax.core.tv.designsystem.TvDimGroup
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOptionRow
import com.filmax.core.tv.designsystem.TvProgressCard
import com.filmax.core.tv.designsystem.TvValueTile
import com.filmax.core.tv.designsystem.rememberTvLazyFocus
import com.filmax.core.tv.designsystem.tvFocusGroup
import com.filmax.core.tv.designsystem.tvLazyFocusGroup
import com.filmax.core.tv.designsystem.tvLazyFocusItem
import com.filmax.feature.player.common.OptionItem
import com.filmax.feature.player.common.PlayerEvent
import com.filmax.feature.player.common.PlayerSeason
import com.filmax.feature.player.common.PlayerState
import com.filmax.feature.player.common.PlayerTab
import com.filmax.feature.player.common.PresetSpeeds
import com.filmax.feature.player.common.episodeMeta
import com.filmax.feature.player.common.episodeProgress
import com.filmax.feature.player.common.episodeTitle
import com.filmax.feature.player.common.optionEvent
import com.filmax.feature.player.common.optionItems
import com.filmax.feature.player.common.playerSeasons
import com.filmax.feature.player.common.shownSeason
import com.filmax.feature.player.common.speedCaption
import com.filmax.feature.player.common.speedValue
import com.filmax.feature.player.common.tabNote

private val OptionGap = 10.dp
private val TileGap = 12.dp
private val EpisodeGap = 18.dp
private val SectionGap = 12.dp

/** Запас вокруг ряда серий под увеличение и свечение фокуса: панель низкая, полный FocusInset не влезает. */
private val EpisodesInset = 8.dp

/**
 * Содержимое вкладки. [contentFocus] — вход в него: при открытии панели и при входе со вкладок
 * фокус встаёт на выбранный вариант — ближайший по геометрии был бы случайным. Запросчик стоит на
 * группе, а не на варианте: в ленивой сетке выбранный за краем не скомпонован, и запрос к нему
 * ронял приложение (2.6.0, два десятка озвучек). Группа сама прокручена к выбранному и входит в него.
 */
@Composable
internal fun PanelTabContent(
    tab: PlayerTab,
    state: PlayerState,
    transport: TvTransportState,
    actions: PlayerActions,
    contentFocus: FocusRequester,
) {
    val series = state.item?.tracklist.orEmpty().size > 1
    when (tab) {
        PlayerTab.Episodes -> EpisodesTab(state, transport.positionMs, actions, contentFocus)
        PlayerTab.Speed -> SpeedTab(state.currentSpeed, actions, contentFocus, tabNote(tab, series))
        PlayerTab.Audio, PlayerTab.Subtitles, PlayerTab.Quality -> OptionsTab(
            items = optionItems(tab, state),
            onSelect = { key -> optionEvent(tab, key)?.let(actions.dispatch) },
            contentFocus = contentFocus,
            note = tabNote(tab, series),
        )
    }
}

/** Серии: сезоны чипами (сезон меняется фокусом на чипе), под ними — серии выбранного сезона. */
@Composable
private fun EpisodesTab(state: PlayerState, positionMs: Long, actions: PlayerActions, contentFocus: FocusRequester) {
    val seasons = remember(state.item) { playerSeasons(state.item?.tracklist.orEmpty()) }
    val shown = state.shownSeason() ?: return
    Column(verticalArrangement = Arrangement.spacedBy(SectionGap)) {
        if (seasons.size > 1) {
            SeasonChips(seasons, shown.number) { season -> actions.dispatch(PlayerEvent.ShowSeason(season)) }
        }
        // Другой сезон — другой набор данных: ряд и его прокрутка собираются заново.
        key(shown.number) {
            EpisodesRow(shown, EpisodesContext(state.track, positionMs, actions), contentFocus)
        }
    }
}

/** Сезоны чипами: первый вход — на показанный сезон, возврат — на чип, с которого ушли. */
@Composable
private fun SeasonChips(seasons: List<PlayerSeason>, shown: Int, onShow: (Int) -> Unit) {
    val start = seasons.indexOfFirst { it.number == shown }.coerceAtLeast(0)
    val rowState = rememberLazyListState(initialFirstVisibleItemIndex = start)
    val seasonFocus = rememberTvLazyFocus()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Сезон", style = FxType.chip, color = LocalFxColors.current.textDim)
        LazyRow(
            state = rowState,
            modifier = Modifier.tvLazyFocusGroup(seasonFocus, rowState, preferred = start),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(EpisodesInset),
        ) {
            itemsIndexed(seasons, key = { _, season -> season.number }) { index, season ->
                TvChip(
                    label = "${season.number}",
                    selected = season.number == shown,
                    onClick = { onShow(season.number) },
                    style = TvChipStyle.CompactOutlined,
                    modifier = Modifier
                        .tvLazyFocusItem(seasonFocus, index)
                        .onFocusChanged { if (it.isFocused) onShow(season.number) },
                )
            }
        }
    }
}

/** Что ряд серий знает о воспроизведении: играющая серия, её позиция и как включить другую. */
private class EpisodesContext(val playing: MediaTrack?, val positionMs: Long, val actions: PlayerActions)

/**
 * Ряд серий сезона. Фокус-цель — играющая серия, а в другом сезоне — первая. OK на играющей просто
 * закрывает панель; на другой — новый экран плеера с её сезоном и номером. Серии без фокуса
 * приглушены, играющая — слабее остальных: её ищут глазами первой.
 */
@Composable
private fun EpisodesRow(season: PlayerSeason, context: EpisodesContext, contentFocus: FocusRequester) {
    TvDimGroup { EpisodeCards(season, context, contentFocus) }
}

@Composable
private fun EpisodeCards(season: PlayerSeason, context: EpisodesContext, contentFocus: FocusRequester) {
    val target = season.episodes.indexOfFirst { it.id == context.playing?.id }.coerceAtLeast(0)
    val rowState = rememberLazyListState(initialFirstVisibleItemIndex = target)
    val episodeFocus = rememberTvLazyFocus()
    LazyRow(
        state = rowState,
        modifier = Modifier.focusRequester(contentFocus).tvLazyFocusGroup(episodeFocus, rowState, preferred = target),
        horizontalArrangement = Arrangement.spacedBy(EpisodeGap),
        contentPadding = PaddingValues(EpisodesInset),
    ) {
        itemsIndexed(season.episodes, key = { _, episode -> episode.id }) { index, episode ->
            val playing = episode.id == context.playing?.id
            TvProgressCard(
                title = episodeTitle(episode),
                meta = episodeMeta(episode, playing, context.positionMs),
                posterUrl = episode.thumbnail,
                progress = episodeProgress(episode),
                onClick = {
                    if (playing) {
                        context.actions.dispatch(PlayerEvent.ClosePanel)
                    } else {
                        context.actions.onPlayEpisode(episode.ref)
                    }
                },
                size = TvCardSize.Panel,
                badge = if (playing) "Сейчас" else null,
                fallbackLabel = "${episode.number}",
                current = playing,
                modifier = Modifier.tvLazyFocusItem(episodeFocus, index),
            )
        }
    }
}

/**
 * Дорожки, субтитры, качество — сеткой в две колонки, выбранный вариант с галочкой. Озвучек у
 * сериала бывает два десятка: сетка открывается прокрученной к выбранной, чтобы фокус встал на неё.
 */
@Composable
private fun OptionsTab(
    items: List<OptionItem>,
    onSelect: (String) -> Unit,
    contentFocus: FocusRequester,
    note: String?
) {
    val selectedIndex = items.indexOfFirst { it.selected }.takeIf { it >= 0 }
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = selectedIndex ?: 0)
    val optionFocus = rememberTvLazyFocus()
    Column(verticalArrangement = Arrangement.spacedBy(SectionGap)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier
                .weight(1f, fill = false)
                .focusRequester(contentFocus)
                .tvLazyFocusGroup(optionFocus, gridState, preferred = selectedIndex),
            horizontalArrangement = Arrangement.spacedBy(OptionGap),
            verticalArrangement = Arrangement.spacedBy(OptionGap),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            itemsIndexed(items, key = { _, item -> item.key }) { index, item ->
                TvOptionRow(
                    title = item.text.title,
                    detail = item.text.detail,
                    badge = item.text.badge,
                    selected = item.selected,
                    onClick = { onSelect(item.key) },
                    modifier = Modifier.tvLazyFocusItem(optionFocus, index),
                )
            }
        }
        note?.let { PanelNote(it) }
    }
}

/**
 * Скорость — ряд крупных плиток: значение видно издалека, выбор — заливкой. Плитки не ленивые,
 * все в композиции — запасной вход на выбранную безопасен, если выбранная есть.
 */
@Composable
private fun SpeedTab(current: Float, actions: PlayerActions, contentFocus: FocusRequester, note: String?) {
    val selectedTile = remember { FocusRequester() }
    val entry = if (current in PresetSpeeds) selectedTile else FocusRequester.Default
    Column(verticalArrangement = Arrangement.spacedBy(SectionGap)) {
        Row(
            Modifier.fillMaxWidth().focusRequester(contentFocus).tvFocusGroup(entry),
            horizontalArrangement = Arrangement.spacedBy(TileGap),
        ) {
            PresetSpeeds.forEach { speed ->
                val selected = speed == current
                TvValueTile(
                    value = speedValue(speed),
                    caption = speedCaption(speed),
                    selected = selected,
                    onClick = { actions.dispatch(PlayerEvent.SetSpeed(speed)) },
                    modifier = Modifier
                        .weight(1f)
                        .then(if (selected) Modifier.focusRequester(selectedTile) else Modifier),
                )
            }
        }
        note?.let { PanelNote(it) }
    }
}

/** Пояснение под вариантами: «Выбор запоминается для всего сериала». */
@Composable
private fun PanelNote(text: String) {
    val color = LocalFxColors.current.textSecondary
    Row(
        Modifier.padding(top = 2.dp, start = TvMetrics.FocusBorderWidth),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(text, style = FxType.meta, color = color)
    }
}

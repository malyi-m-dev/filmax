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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import com.filmax.core.tv.designsystem.tvFocusGroup
import com.filmax.core.ui.components.formatPlayerTime
import com.filmax.feature.player.common.OptionItem
import com.filmax.feature.player.common.PlayerEvent
import com.filmax.feature.player.common.PlayerSeason
import com.filmax.feature.player.common.PlayerState
import com.filmax.feature.player.common.PlayerTab
import com.filmax.feature.player.common.PresetSpeeds
import com.filmax.feature.player.common.WATCH_STATUS_FINISHED
import com.filmax.feature.player.common.episodeLength
import com.filmax.feature.player.common.episodeProgress
import com.filmax.feature.player.common.episodeTitle
import com.filmax.feature.player.common.optionEvent
import com.filmax.feature.player.common.optionItems
import com.filmax.feature.player.common.playerSeasons
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
 * Содержимое вкладки. [contentFocus] стоит на выбранном варианте: на него фокус встаёт при
 * открытии панели и при входе в содержимое со вкладок — ближайший по геометрии вариант был бы
 * случайным.
 */
@Composable
internal fun PanelTabContent(
    tab: PlayerTab,
    state: PlayerState,
    onEvent: (PlayerEvent) -> Unit,
    contentFocus: FocusRequester,
) {
    when (tab) {
        PlayerTab.Episodes -> EpisodesTab(state, onEvent, contentFocus)
        PlayerTab.Speed -> SpeedTab(state.currentSpeed, onEvent, contentFocus, tabNote(tab, state.isSeries))
        PlayerTab.Audio, PlayerTab.Subtitles, PlayerTab.Quality -> OptionsTab(
            items = optionItems(tab, state),
            onSelect = { key -> optionEvent(tab, key)?.let(onEvent) },
            contentFocus = contentFocus,
            note = tabNote(tab, state.isSeries),
        )
    }
}

/** Серии: сезоны чипами (сезон меняется фокусом на чипе), под ними — серии выбранного сезона. */
@Composable
private fun EpisodesTab(state: PlayerState, onEvent: (PlayerEvent) -> Unit, contentFocus: FocusRequester) {
    val seasons = remember(state.item) { playerSeasons(state.item?.tracklist.orEmpty()) }
    val shown = state.shownSeason() ?: return
    Column(verticalArrangement = Arrangement.spacedBy(SectionGap)) {
        if (seasons.size > 1) {
            SeasonChips(seasons, shown.number) { season -> onEvent(PlayerEvent.ShowSeason(season)) }
        }
        // Другой сезон — другой набор данных: ряд и его прокрутка собираются заново.
        key(shown.number) {
            EpisodesRow(shown, state, onEvent, contentFocus)
        }
    }
}

@Composable
private fun SeasonChips(seasons: List<PlayerSeason>, shown: Int, onShow: (Int) -> Unit) {
    val selectedFocus = remember { FocusRequester() }
    val start = seasons.indexOfFirst { it.number == shown }.coerceAtLeast(0)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Сезон", style = FxType.chip, color = LocalFxColors.current.textDim)
        LazyRow(
            state = rememberLazyListState(initialFirstVisibleItemIndex = start),
            modifier = Modifier.tvFocusGroup(selectedFocus),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(EpisodesInset),
        ) {
            items(seasons, key = { it.number }) { season ->
                val selected = season.number == shown
                TvChip(
                    label = "${season.number}",
                    selected = selected,
                    onClick = { onShow(season.number) },
                    style = TvChipStyle.CompactOutlined,
                    modifier = (if (selected) Modifier.focusRequester(selectedFocus) else Modifier)
                        .onFocusChanged { if (it.isFocused) onShow(season.number) },
                )
            }
        }
    }
}

/**
 * Ряд серий сезона. Фокус-цель — играющая серия, а в другом сезоне — первая. OK на играющей просто
 * закрывает панель; на другой — новый экран плеера с её сезоном и номером. Серии без фокуса
 * приглушены, играющая — слабее остальных: её ищут глазами первой.
 */
@Composable
private fun EpisodesRow(
    season: PlayerSeason,
    state: PlayerState,
    onEvent: (PlayerEvent) -> Unit,
    contentFocus: FocusRequester,
) {
    TvDimGroup { EpisodeCards(season, state, onEvent, contentFocus) }
}

@Composable
private fun EpisodeCards(
    season: PlayerSeason,
    state: PlayerState,
    onEvent: (PlayerEvent) -> Unit,
    contentFocus: FocusRequester,
) {
    val playingId = state.track?.id
    val target = season.episodes.indexOfFirst { it.id == playingId }.coerceAtLeast(0)
    LazyRow(
        state = rememberLazyListState(initialFirstVisibleItemIndex = target),
        modifier = Modifier.tvFocusGroup(contentFocus),
        horizontalArrangement = Arrangement.spacedBy(EpisodeGap),
        contentPadding = PaddingValues(EpisodesInset),
    ) {
        itemsIndexed(season.episodes, key = { _, episode -> episode.id }) { index, episode ->
            val playing = episode.id == playingId
            TvProgressCard(
                title = episodeTitle(episode),
                meta = episodeMeta(episode, playing, state.playback.positionMs),
                posterUrl = episode.thumbnail,
                progress = episodeProgress(episode),
                onClick = {
                    if (playing) {
                        onEvent(PlayerEvent.ClosePanel)
                    } else {
                        onEvent(PlayerEvent.PlayEpisode(episode.seasonNumber, episode.number))
                    }
                },
                size = TvCardSize.Panel,
                badge = if (playing) "Сейчас" else null,
                fallbackLabel = "${episode.number}",
                current = playing,
                modifier = if (index == target) Modifier.focusRequester(contentFocus) else Modifier,
            )
        }
    }
}

/**
 * Подпись под кадром серии: играющая — «Идёт · 34:50», досмотренная — «Просмотрено», остальные —
 * длительность. Длительности нет — подписи нет.
 */
internal fun episodeMeta(episode: MediaTrack, playing: Boolean, positionMs: Long): String? = when {
    playing -> "Идёт · ${formatPlayerTime(positionMs)}"
    episode.watchStatus == WATCH_STATUS_FINISHED -> "Просмотрено"
    else -> episodeLength(episode)
}

/** Дорожки, субтитры, качество — сеткой в две колонки, выбранный вариант с галочкой. */
@Composable
private fun OptionsTab(
    items: List<OptionItem>,
    onSelect: (String) -> Unit,
    contentFocus: FocusRequester,
    note: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SectionGap)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f, fill = false).tvFocusGroup(contentFocus),
            horizontalArrangement = Arrangement.spacedBy(OptionGap),
            verticalArrangement = Arrangement.spacedBy(OptionGap),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(items, key = { it.key }) { item ->
                TvOptionRow(
                    title = item.text.title,
                    detail = item.text.detail,
                    badge = item.text.badge,
                    selected = item.selected,
                    onClick = { onSelect(item.key) },
                    modifier = if (item.selected) Modifier.focusRequester(contentFocus) else Modifier,
                )
            }
        }
        note?.let { PanelNote(it) }
    }
}

/** Скорость — ряд крупных плиток: значение видно издалека, выбор — заливкой. */
@Composable
private fun SpeedTab(current: Float, onEvent: (PlayerEvent) -> Unit, contentFocus: FocusRequester, note: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(SectionGap)) {
        Row(Modifier.fillMaxWidth().tvFocusGroup(contentFocus), horizontalArrangement = Arrangement.spacedBy(TileGap)) {
            PresetSpeeds.forEach { speed ->
                val selected = speed == current
                TvValueTile(
                    value = speedValue(speed),
                    caption = speedCaption(speed),
                    selected = selected,
                    onClick = { onEvent(PlayerEvent.SetSpeed(speed)) },
                    modifier = Modifier
                        .weight(1f)
                        .then(if (selected) Modifier.focusRequester(contentFocus) else Modifier),
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

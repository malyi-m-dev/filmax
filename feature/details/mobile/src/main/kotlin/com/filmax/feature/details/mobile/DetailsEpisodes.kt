// Серии сериала в деталях: сезоны чипами и строки серий выбранного сезона. Сезон, строки и их
// подписи считает модель (DetailsState.episodes) — на телефоне тап применяется сразу.

package com.filmax.feature.details.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.ui.components.EpisodeMark
import com.filmax.core.ui.components.FilmaxChip
import com.filmax.core.ui.components.FilmaxEpisodeRow
import com.filmax.core.ui.components.FilmaxSectionHeader
import com.filmax.feature.details.common.DetailsEvent
import com.filmax.feature.details.common.EpisodeRow
import com.filmax.feature.details.common.EpisodesPanel

internal fun LazyListScope.episodesSection(panel: EpisodesPanel, onEvent: (DetailsEvent) -> Unit) {
    if (panel.seasons.isEmpty()) return
    item(key = "episodes-title") {
        FilmaxSectionHeader(title = "Серии", count = panel.rows.size.takeIf { it > 0 })
    }
    // Один сезон — чипы не нужны: выбирать не из чего.
    if (panel.seasons.size > 1) {
        item(key = "seasons") { SeasonChips(panel, onEvent) }
    }
    items(panel.rows, key = { "episode-${it.episode.id}" }) { row ->
        EpisodeRow(row = row, onEvent = onEvent)
    }
}

@Composable
private fun SeasonChips(panel: EpisodesPanel, onEvent: (DetailsEvent) -> Unit) {
    // Открыли сериал на четвёртом сезоне — его чип выезжает в ряд, а не прячется за краем.
    val chips = rememberLazyListState()
    val selectedIndex = panel.seasons.indexOfFirst { it.selected }.coerceAtLeast(0)
    LaunchedEffect(selectedIndex) { chips.animateScrollToItem(selectedIndex) }
    LazyRow(
        state = chips,
        contentPadding = PaddingValues(horizontal = FilmaxMetrics.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        items(panel.seasons, key = { it.number }) { season ->
            FilmaxChip(
                label = season.label,
                selected = season.selected,
                onClick = { onEvent(DetailsEvent.SwitchSeason(season.number)) },
            )
        }
    }
}

@Composable
private fun EpisodeRow(row: EpisodeRow, onEvent: (DetailsEvent) -> Unit) {
    val episode = row.episode
    FilmaxEpisodeRow(
        title = row.title,
        meta = row.meta,
        thumbUrl = episode.thumbnail,
        mark = when {
            row.resume -> EpisodeMark.Current
            row.watched -> EpisodeMark.Watched
            else -> EpisodeMark.Normal
        },
        progress = row.progress ?: 0f,
        // Второй аргумент — НОМЕР серии: номер уникален только внутри сезона.
        onClick = { onEvent(DetailsEvent.PlayEpisode(episode.seasonNumber, episode.number)) },
        placeholder = episode.number.toString(),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
    )
}

// Серии сериала в деталях: сезоны чипами и строки серий выбранного сезона. Сезон выбирает модель
// (DetailsState.selectedSeason) — на телефоне тап применяется сразу, без выжидания ТВ-фокуса.

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
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.ui.components.EpisodeMark
import com.filmax.core.ui.components.FilmaxChip
import com.filmax.core.ui.components.FilmaxEpisodeRow
import com.filmax.core.ui.components.FilmaxSectionHeader
import com.filmax.feature.details.common.DetailsEvent
import com.filmax.feature.details.common.DetailsState
import com.filmax.feature.details.common.WATCH_STATUS_FINISHED
import com.filmax.feature.details.common.episodeProgress
import com.filmax.feature.details.common.episodeRowMeta
import com.filmax.feature.details.common.numberedEpisodeTitle
import com.filmax.feature.details.common.seasonLabel

internal fun LazyListScope.episodesSection(state: DetailsState, actions: DetailsActions) {
    val seasons = state.series?.seasons.orEmpty()
    if (seasons.isEmpty()) return
    val resumeId = state.series?.resume?.id
    item(key = "episodes-title") {
        FilmaxSectionHeader(title = "Серии", count = state.seasonEpisodes.size.takeIf { it > 0 })
    }
    // Один сезон — чипы не нужны: выбирать не из чего.
    if (seasons.size > 1) {
        item(key = "seasons") {
            // Открыли сериал на четвёртом сезоне — его чип выезжает в ряд, а не прячется за краем.
            val chips = rememberLazyListState()
            val selectedIndex = seasons.indexOfFirst { it.first == state.selectedSeason }.coerceAtLeast(0)
            LaunchedEffect(selectedIndex) { chips.animateScrollToItem(selectedIndex) }
            LazyRow(
                state = chips,
                contentPadding = PaddingValues(horizontal = FilmaxMetrics.ScreenPadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                items(seasons, key = { it.first }) { (number, _) ->
                    FilmaxChip(
                        label = seasonLabel(number),
                        selected = number == state.selectedSeason,
                        onClick = { actions.onDetails(DetailsEvent.SwitchSeason(number)) },
                    )
                }
            }
        }
    }
    items(state.seasonEpisodes, key = { "episode-${it.id}" }) { episode ->
        EpisodeRow(episode = episode, resumeId = resumeId, onPlay = actions.onPlay)
    }
}

@Composable
private fun EpisodeRow(episode: MediaTrack, resumeId: Int?, onPlay: (season: Int, videoId: Int) -> Unit) {
    val watched = episode.watchStatus == WATCH_STATUS_FINISHED
    val current = episode.id == resumeId && !watched
    FilmaxEpisodeRow(
        title = numberedEpisodeTitle(episode),
        meta = episodeRowMeta(episode, resume = current),
        thumbUrl = episode.thumbnail,
        mark = when {
            current -> EpisodeMark.Current
            watched -> EpisodeMark.Watched
            else -> EpisodeMark.Normal
        },
        progress = episodeProgress(episode),
        // Второй аргумент — НОМЕР серии: номер уникален только внутри сезона.
        onClick = { onPlay(episode.seasonNumber, episode.number) },
        placeholder = episode.number.toString(),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
    )
}

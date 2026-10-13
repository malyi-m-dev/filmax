// Сериальная часть экрана деталей: выбор сезона и серии выбранного сезона.

package com.filmax.feature.details.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxOnSurfaceDim
import com.filmax.core.designsystem.ShapeFull
import com.filmax.core.designsystem.ShapePoster
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.ui.components.FilmaxProgressBar
import com.filmax.core.ui.components.PosterImage
import com.filmax.feature.details.common.WATCH_STATUS_FINISHED
import com.filmax.feature.details.common.WATCH_STATUS_IN_PROGRESS

private const val SECONDS_IN_MINUTE = 60

private const val EPISODES_TITLE = "Эпизоды"

/** Данные и действия секции эпизодов — группой (detekt LongParameterList). */
internal data class EpisodesSectionData(
    val seasons: List<Pair<Int, List<MediaTrack>>>,
    val episodes: List<MediaTrack>,
    val selectedSeason: Int,
    val onSelectSeason: (Int) -> Unit,
    val onPlayEpisode: (season: Int, videoId: Int) -> Unit,
)

@Composable
internal fun EpisodesSection(data: EpisodesSectionData, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = FilmaxMetrics.DetailsPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                EPISODES_TITLE,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            // Селектор — только когда сезонов больше одного: меню из единственного пункта ничего
            // не выбирает, а место и внимание забирает.
            if (data.seasons.size > 1) {
                SeasonSelector(
                    seasons = data.seasons,
                    selected = data.selectedSeason,
                    onSelect = data.onSelectSeason,
                )
            }
        }
        Column(
            Modifier.padding(top = 16.dp, start = FilmaxMetrics.DetailsPadding, end = FilmaxMetrics.DetailsPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            data.episodes.forEach { episode ->
                EpisodeRow(
                    episode = episode,
                    // Плееру нужен НОМЕР серии (API `video`), а не id трека.
                    onClick = { data.onPlayEpisode(episode.seasonNumber, episode.number) },
                )
            }
        }
    }
}

@Composable
private fun SeasonSelector(
    seasons: List<Pair<Int, List<MediaTrack>>>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .height(34.dp)
                .clip(ShapeFull)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable { expanded = true }
                .padding(start = 14.dp, end = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                seasonLabel(seasons.getOrNull(selected)?.first ?: 0),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            seasons.forEachIndexed { index, season ->
                DropdownMenuItem(
                    text = {
                        Text(
                            seasonLabel(season.first),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (index == selected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun EpisodeRow(episode: MediaTrack, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        EpisodeThumb(episode)
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    episodeNumberLabel(episode),
                    style = MaterialTheme.typography.bodySmall,
                    color = FilmaxOnSurfaceDim,
                )
                episodeBadge(episode)?.let { badge ->
                    Text(
                        badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                episode.title.ifBlank { "Серия ${episode.number}" },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
            episodeDuration(episode)?.let { duration ->
                Text(
                    duration,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun EpisodeThumb(episode: MediaTrack) {
    Box(
        Modifier
            .size(width = FilmaxMetrics.EpisodeThumbWidth, height = FilmaxMetrics.EpisodeThumbHeight)
            .clip(ShapePoster)
            .background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center,
    ) {
        // У kino.pub thumbnail часто пустой, и пустая плитка не отличима от соседней — тогда
        // показываем крупный номер серии.
        if (episode.thumbnail.isNotBlank()) {
            PosterImage(
                url = episode.thumbnail,
                contentDescription = episode.title,
                modifier = Modifier.fillMaxSize(),
                shape = ShapePoster,
                accentColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
        } else {
            Text(
                "${episode.number}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilmaxProgressBar(
            progress = episodeProgress(episode),
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

/** «Сезон 1»; у тайтла без сезонов (kino.pub отдаёт 0) сезона нет — есть просто серии. */
private fun seasonLabel(number: Int): String = if (number > 0) "Сезон $number" else "Серии"

/** «S1 · E3» — как в макете; без сезона остаётся только номер серии. */
private fun episodeNumberLabel(episode: MediaTrack): String =
    if (episode.seasonNumber > 0) "S${episode.seasonNumber} · E${episode.number}" else "E${episode.number}"

/** Бейдж по `watching.status`: досмотрен, в процессе или (не начат) — ничего. */
private fun episodeBadge(episode: MediaTrack): String? = when (episode.watchStatus) {
    WATCH_STATUS_FINISHED -> "просмотрено"
    WATCH_STATUS_IN_PROGRESS -> "смотрите"
    else -> null
}

private fun episodeDuration(episode: MediaTrack): String? =
    episode.durationSeconds.takeIf { it > 0 }?.let { "${it / SECONDS_IN_MINUTE} мин" }

private fun episodeProgress(episode: MediaTrack): Float =
    if (episode.durationSeconds > 0) episode.watchedSeconds.toFloat() / episode.durationSeconds else 0f

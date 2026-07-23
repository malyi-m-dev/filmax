package com.filmax.feature.player.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.tv.designsystem.TvAccent
import com.filmax.core.tv.designsystem.TvOnAccent
import com.filmax.core.tv.designsystem.TvOnSurface
import com.filmax.core.tv.designsystem.TvOnSurfaceDim
import com.filmax.core.tv.designsystem.TvOnSurfaceVariant
import com.filmax.core.tv.designsystem.TvSurface
import com.filmax.core.tv.designsystem.TvSurfaceContainerHighest

/**
 * Боковая панель сезонов и серий. Рядов-кнопок с фокусом здесь нет — как и всюду в плеере,
 * курсор ведёт обработчик клавиш: подсветка — [episodeCursor], сезон меняется ◄/►.
 * У каждой серии — полоса просмотра (как в «Моё») и отметка «Сейчас» у играющей.
 */
@Composable
internal fun EpisodesPanel(
    panel: EpisodesPanelData,
    seasonCursor: Int,
    episodeCursor: Int,
    modifier: Modifier = Modifier,
) {
    val season = panel.seasons.getOrNull(seasonCursor) ?: return
    val listState = rememberLazyListState()
    // Курсор всегда в кадре: список едет за клавишами, включая стартовую позицию «Сейчас».
    LaunchedEffect(seasonCursor, episodeCursor) { listState.animateScrollToItem(episodeCursor) }

    Column(
        modifier
            .width(EpisodesPanelWidth)
            .playerPanel()
            .padding(14.dp),
    ) {
        EpisodesPanelHeader(
            seasonNumber = season.first,
            hasPrev = seasonCursor > 0,
            hasNext = seasonCursor < panel.seasons.lastIndex,
        )
        Text(
            "◄ ► сезон · OK — смотреть",
            style = MaterialTheme.typography.labelSmall,
            color = TvOnSurfaceDim,
            modifier = Modifier.padding(top = 3.dp, start = 8.dp),
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(season.second, key = { _, episode -> episode.id }) { index, episode ->
                EpisodePanelRow(
                    episode = episode,
                    highlighted = index == episodeCursor,
                    isCurrent = episode.id == panel.currentTrackId,
                )
            }
        }
    }
}

/** Шапка панели: «Сезон N» и стрелки-подсказки только в те стороны, где сезоны есть. */
@Composable
private fun EpisodesPanelHeader(seasonNumber: Int, hasPrev: Boolean, hasNext: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "‹",
            style = MaterialTheme.typography.titleMedium,
            color = if (hasPrev) TvOnSurface else TvSurfaceContainerHighest,
        )
        Text(
            if (seasonNumber > 0) "Сезон $seasonNumber" else "Серии",
            style = MaterialTheme.typography.titleMedium,
            color = TvOnSurface,
        )
        Text(
            "›",
            style = MaterialTheme.typography.titleMedium,
            color = if (hasNext) TvOnSurface else TvSurfaceContainerHighest,
        )
    }
}

/** [highlighted] — под курсором, [isCurrent] — серия, которая играет сейчас. Это разные вещи. */
@Composable
private fun EpisodePanelRow(episode: MediaTrack, highlighted: Boolean, isCurrent: Boolean) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(if (highlighted) TvAccent else TvSurface.copy(alpha = 0f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "${episode.number}. ${episode.title.ifBlank { "Серия ${episode.number}" }}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
                color = if (highlighted) TvOnAccent else TvOnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Text(
                text = if (isCurrent) "Сейчас" else episodeDurationLabel(episode),
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    highlighted -> TvOnAccent
                    isCurrent -> TvAccent
                    else -> TvOnSurfaceVariant
                },
                modifier = Modifier.padding(start = 10.dp),
            )
        }
        EpisodeWatchBar(episode = episode, highlighted = highlighted)
    }
}

/**
 * Полоса просмотра серии — как на карточках «Моё»: трек виден у КАЖДОЙ серии (у непросмотренной
 * он пустой), заполнение — сколько досмотрено. Так список читается как история просмотра.
 */
@Composable
private fun EpisodeWatchBar(episode: MediaTrack, highlighted: Boolean) {
    val fraction = episodeWatchFraction(episode)
    val barColor = if (highlighted) TvOnAccent else TvAccent
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 7.dp)
            .height(3.dp)
            .clip(CircleShape)
            .background(barColor.copy(alpha = 0.25f)),
    ) {
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(3.dp)
                    .background(barColor),
            )
        }
    }
}

/** Доля просмотра серии: досмотренная — всегда полная полоса, даже если время чуть меньше конца. */
private fun episodeWatchFraction(episode: MediaTrack): Float = when {
    episode.watchStatus == WATCH_STATUS_FINISHED -> 1f
    episode.durationSeconds > 0 -> (episode.watchedSeconds.toFloat() / episode.durationSeconds).coerceIn(0f, 1f)
    else -> 0f
}

private fun episodeDurationLabel(episode: MediaTrack): String =
    episode.durationSeconds.takeIf { it > 0 }?.let { "${it / SECONDS_IN_MINUTE} мин" }.orEmpty()

/** `watching.status` из API: 1 — серия досмотрена до конца. */
private const val WATCH_STATUS_FINISHED = 1

private const val SECONDS_IN_MINUTE = 60

/** Ширина боковой панели серий. */
private val EpisodesPanelWidth = 330.dp

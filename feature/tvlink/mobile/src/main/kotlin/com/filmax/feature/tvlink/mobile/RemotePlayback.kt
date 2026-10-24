// Пульт: что играет на телевизоре — постер, название, серия, перемотка и кнопки управления.

package com.filmax.feature.tvlink.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.domain.tvlink.PlaybackStatus
import com.filmax.core.ui.components.FilmaxButton
import com.filmax.core.ui.components.FilmaxButtonStyle
import com.filmax.core.ui.components.FilmaxRoundButton
import com.filmax.core.ui.components.FilmaxSeekBar
import com.filmax.core.ui.components.PosterImage
import com.filmax.feature.tvlink.common.RemoteEvent
import com.filmax.feature.tvlink.common.episodeLabel

/** Шаг перемотки — тот же, что у кнопок плеера (±10 с). */
private const val SEEK_STEP_MS = 10_000L

/** Постер 2:3 крупнее, чем в лентах: на пульте он — главное, что видно. */
private val PosterMaxHeight = 252.dp
private const val POSTER_ASPECT = 2f / 3f

/**
 * Что играет и управление. Постер занимает то, что осталось от кнопок: на маленьком телефоне он
 * сжимается, а кнопки не уезжают за край.
 */
@Composable
internal fun RemotePlayback(
    playback: PlaybackStatus,
    positionMs: Long,
    dispatch: (RemoteEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        PosterImage(
            url = playback.posterUrl.orEmpty(),
            contentDescription = playback.title,
            modifier = Modifier
                .weight(1f, fill = false)
                .heightIn(max = PosterMaxHeight)
                .aspectRatio(POSTER_ASPECT, matchHeightConstraintsFirst = true),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                playback.title,
                style = FilmaxType.profileName,
                color = FilmaxColors.Text,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            playbackCaption(playback)?.let {
                Text(
                    it,
                    style = FilmaxType.meta,
                    color = FilmaxColors.TextSecondary,
                )
            }
        }
        FilmaxSeekBar(
            positionMs = positionMs,
            durationMs = playback.durationMs,
            onScrub = { dispatch(RemoteEvent.Scrub(it)) },
            onScrubFinished = { dispatch(RemoteEvent.ScrubFinished) },
        )
        Transport(playback, dispatch)
        Episodes(playback, dispatch)
        FilmaxButton(
            text = "Остановить на телевизоре",
            onClick = { dispatch(RemoteEvent.StopOnTv) },
            icon = Icons.Filled.Stop,
            style = FilmaxButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Серия, а пока поток грузится — «Загружается»: иначе пауза и загрузка на пульте неотличимы. */
private fun playbackCaption(playback: PlaybackStatus): String? =
    listOfNotNull(playback.episodeLabel(), "Загружается".takeIf { playback.buffering })
        .joinToString(" · ")
        .ifEmpty { null }

@Composable
private fun Transport(playback: PlaybackStatus, dispatch: (RemoteEvent) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
        FilmaxRoundButton(
            icon = Icons.Filled.Replay10,
            contentDescription = "Назад 10 секунд",
            onClick = { dispatch(RemoteEvent.SeekBy(-SEEK_STEP_MS)) },
        )
        FilmaxRoundButton(
            icon = if (playback.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (playback.playing) "Пауза" else "Смотреть",
            onClick = { dispatch(RemoteEvent.TogglePlay) },
            emphasized = true,
        )
        FilmaxRoundButton(
            icon = Icons.Filled.Forward10,
            contentDescription = "Вперёд 10 секунд",
            onClick = { dispatch(RemoteEvent.SeekBy(SEEK_STEP_MS)) },
        )
    }
}

/** Соседние серии — только у сериала; кнопки нет, если серии с той стороны нет. */
@Composable
private fun Episodes(playback: PlaybackStatus, dispatch: (RemoteEvent) -> Unit) {
    if (!playback.hasPrevious && !playback.hasNext) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (playback.hasPrevious) {
            FilmaxButton(
                text = "Предыдущая",
                onClick = { dispatch(RemoteEvent.PreviousEpisode) },
                icon = Icons.Filled.SkipPrevious,
                style = FilmaxButtonStyle.Secondary,
                modifier = Modifier.weight(1f),
            )
        }
        if (playback.hasNext) {
            FilmaxButton(
                text = "Следующая",
                onClick = { dispatch(RemoteEvent.NextEpisode) },
                icon = Icons.Filled.SkipNext,
                style = FilmaxButtonStyle.Secondary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// Подписи ТВ-плеера — чистые функции без UI: остаток времени, иконки вкладок, подсказки пульта
// и подписи серий. Общие с телефоном подписи и варианты вкладок — в player.common (PlayerLabels, PlayerOptions).
package com.filmax.feature.player.tv

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.ui.components.formatPlayerTime
import com.filmax.feature.player.common.PlayerTab

/** `watching.status` kino.pub: 1 — досмотрено. */
private const val WATCH_STATUS_FINISHED = 1
private const val SECONDS_IN_MINUTE = 60

/** Остаток до конца: «−21:48». Длительность неизвестна — пусто, а не «−0:00». */
internal fun remainingLabel(positionMs: Long, durationMs: Long): String =
    if (durationMs <= 0) "" else "−" + formatPlayerTime((durationMs - positionMs).coerceAtLeast(0))

internal val PlayerTab.icon: ImageVector
    get() = when (this) {
        PlayerTab.Episodes -> Icons.Rounded.VideoLibrary
        PlayerTab.Audio -> Icons.Rounded.GraphicEq
        PlayerTab.Subtitles -> Icons.Rounded.Subtitles
        PlayerTab.Quality -> Icons.Rounded.Hd
        PlayerTab.Speed -> Icons.Rounded.Speed
    }

/** Что делают ←/→ во вкладке — подсказка внизу панели. */
internal val PlayerTab.moveHint: String
    get() = when (this) {
        PlayerTab.Episodes -> "серия"
        PlayerTab.Audio -> "дорожка"
        PlayerTab.Subtitles, PlayerTab.Quality -> "вариант"
        PlayerTab.Speed -> "скорость"
    }

/** «7. Тайный совет»; без названия — «7. Серия 7»: у kino.pub названий серий часто нет. */
internal fun episodeTitle(episode: MediaTrack): String =
    "${episode.number}. ${episode.title.ifBlank { "Серия ${episode.number}" }}"

/**
 * Подпись под кадром серии: играющая — «Идёт · 34:50», досмотренная — «Просмотрено», остальные —
 * длительность. Длительности нет — подписи нет.
 */
internal fun episodeMeta(episode: MediaTrack, playing: Boolean, positionMs: Long): String? = when {
    playing -> "Идёт · ${formatPlayerTime(positionMs)}"
    episode.watchStatus == WATCH_STATUS_FINISHED -> "Просмотрено"
    episode.durationSeconds > 0 -> "${episode.durationSeconds / SECONDS_IN_MINUTE} мин"
    else -> null
}

/** Полоса просмотра серии: досмотренная — полная, начатая — доля, не начатая — без полосы. */
internal fun episodeProgress(episode: MediaTrack): Float? = when {
    episode.watchStatus == WATCH_STATUS_FINISHED -> 1f
    episode.watchedSeconds > 0 && episode.durationSeconds > 0 ->
        (episode.watchedSeconds.toFloat() / episode.durationSeconds).coerceIn(0f, 1f)
    else -> null
}

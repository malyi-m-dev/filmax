// Подписи серий в плеере — общие для телефона и ТВ: код серии для «Далее», название и длительность
// в панели, полоса досмотренного. Раньше телефон собирал их в вёрстке, а ТВ — своими функциями.
package com.filmax.feature.player.common

import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.presentation.format.formatPlayerTime

/** `watching.status` kino.pub: 1 — досмотрено. */
const val WATCH_STATUS_FINISHED = 1
private const val SECONDS_IN_MINUTE = 60

/** «S4E8»; серия вне сезона — «Серия 8»: чип «Далее» на телефоне. */
fun episodeCode(episode: MediaTrack): String =
    if (episode.seasonNumber > 0) "S${episode.seasonNumber}E${episode.number}" else "Серия ${episode.number}"

/** «7. Тайный совет»; без названия — «7. Серия 7»: у kino.pub названий серий часто нет. */
fun episodeTitle(episode: MediaTrack): String =
    "${episode.number}. ${episode.title.ifBlank { "Серия ${episode.number}" }}"

/**
 * Подпись под кадром серии на ТВ: играющая — «Идёт · 34:50», досмотренная — «Просмотрено»,
 * остальные — длительность. Длительности нет — подписи нет.
 */
fun episodeMeta(episode: MediaTrack, playing: Boolean, positionMs: Long): String? = when {
    playing -> "Идёт · ${formatPlayerTime(positionMs)}"
    episode.watchStatus == WATCH_STATUS_FINISHED -> "Просмотрено"
    else -> episodeLength(episode)
}

/** «58 мин» — длительность серии; неизвестна — null. */
fun episodeLength(episode: MediaTrack): String? =
    (episode.durationSeconds / SECONDS_IN_MINUTE).takeIf { it > 0 }?.let { "$it мин" }

/** Полоса просмотра серии: досмотренная — полная, начатая — доля, не начатая — без полосы. */
fun episodeProgress(episode: MediaTrack): Float? = when {
    episode.watchStatus == WATCH_STATUS_FINISHED -> 1f
    episode.watchedSeconds > 0 && episode.durationSeconds > 0 ->
        (episode.watchedSeconds.toFloat() / episode.durationSeconds).coerceIn(0f, 1f)
    else -> null
}

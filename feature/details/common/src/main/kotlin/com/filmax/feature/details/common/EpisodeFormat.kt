// Подписи и позиция серий — общие для телефона и ТВ: раньше у каждой платформы была своя
// episodeMeta, и они разошлись (на ТВ досмотренная серия не отличалась от нетронутой).

package com.filmax.feature.details.common

import com.filmax.core.domain.catalog.model.MediaTrack

private const val SECONDS_IN_MINUTE = 60

/** Название серии, а у безымянной — её номер. */
fun episodeTitle(episode: MediaTrack): String = episode.title.ifBlank { "Серия ${episode.number}" }

/** Название строки в списке серий: «7. Тайный совет»; безымянная — «Серия 7». */
fun numberedEpisodeTitle(episode: MediaTrack): String =
    if (episode.title.isBlank()) "Серия ${episode.number}" else "${episode.number}. ${episode.title}"

/**
 * Мета строки серии без номера — он уже в названии: «Просмотрено · 52 мин», «Продолжить ·
 * осталось 41 мин», у начатой — остаток, у нетронутой — длительность. [resume] — серия «продолжить».
 */
fun episodeRowMeta(episode: MediaTrack, resume: Boolean): String? {
    val total = (episode.durationSeconds / SECONDS_IN_MINUTE).takeIf { it > 0 }?.let(::durationLabel)
    val left = remainingLabel(episode)
    return when {
        episode.watchStatus == WATCH_STATUS_FINISHED -> listOfNotNull("Просмотрено", total).joinToString(" · ")
        resume -> listOfNotNull("Продолжить", left).joinToString(" · ")
        episode.watchStatus == WATCH_STATUS_IN_PROGRESS -> left
        else -> total
    }
}

/**
 * Подпись под карточкой серии: «Серия 3 · осталось 18 мин». Номер — если он не стал заголовком,
 * дальше — где в серии зритель: досмотрена, сколько осталось в начатой, длительность нетронутой.
 */
fun episodeMeta(episode: MediaTrack): String? = buildList {
    if (episode.title.isNotBlank()) add("Серия ${episode.number}")
    episodeState(episode)?.let(::add)
}.joinToString(" · ").ifBlank { null }

/** Полоса прогресса серии. Досмотренная — полная, даже если kino.pub не прислал время. */
fun episodeProgress(episode: MediaTrack): Float = when {
    episode.watchStatus == WATCH_STATUS_FINISHED -> 1f
    episode.durationSeconds > 0 -> (episode.watchedSeconds.toFloat() / episode.durationSeconds).coerceIn(0f, 1f)
    else -> 0f
}

/**
 * С какой серии открыть ряд сезона: «продолжить», если она в этом сезоне, иначе первая
 * недосмотренная. От начала сезона к ней пришлось бы листать вручную — на ТВ это десяток
 * нажатий пульта на каждом заходе.
 */
fun startEpisodeIndex(episodes: List<MediaTrack>, resumeId: Int?): Int {
    val resumeIndex = episodes.indexOfFirst { it.id == resumeId }
    if (resumeIndex >= 0) return resumeIndex
    return episodes.indexOfFirst { it.watchStatus != WATCH_STATUS_FINISHED }.coerceAtLeast(0)
}

/** Серия, на которой откроется сезон [season] (см. [startEpisodeIndex]); null — такого сезона нет. */
fun SeriesData.startEpisode(season: Int): MediaTrack? {
    val episodes = seasons.firstOrNull { it.first == season }?.second.orEmpty()
    return episodes.getOrNull(startEpisodeIndex(episodes, resume?.id))
}

/** «Сезон 1»; у тайтла без сезонов (kino.pub отдаёт 0) сезона нет — есть просто серии. */
fun seasonLabel(number: Int): String = if (number > 0) "Сезон $number" else "Серии"

/** Досмотрена, сколько осталось в начатой или длительность нетронутой. */
private fun episodeState(episode: MediaTrack): String? = when (episode.watchStatus) {
    WATCH_STATUS_FINISHED -> "просмотрено"
    WATCH_STATUS_IN_PROGRESS -> remainingLabel(episode) ?: "смотрите"
    else -> (episode.durationSeconds / SECONDS_IN_MINUTE).takeIf { it > 0 }?.let(::durationLabel)
}

private fun remainingLabel(episode: MediaTrack): String? {
    val remainingMinutes = (episode.durationSeconds - episode.watchedSeconds) / SECONDS_IN_MINUTE
    return remainingMinutes.takeIf { it > 0 }?.let { "осталось ${durationLabel(it)}" }
}

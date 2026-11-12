// Подписи деталей телефона — чистые функции без UI: надзаголовок, что и с какого места играет
// главная кнопка и её подпись. Общие с ТВ правила (серии, факты, объём) живут в details.common.

package com.filmax.feature.details.mobile

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.catalog.model.VideoRef
import com.filmax.core.domain.catalog.model.ref
import com.filmax.core.presentation.format.durationLabel
import com.filmax.core.presentation.format.typeLabel
import com.filmax.feature.details.common.SeriesData
import com.filmax.feature.details.common.episodeProgress
import com.filmax.feature.details.common.hasUhd
import com.filmax.feature.details.common.volumeLabel

private const val SECONDS_IN_MINUTE = 60

/**
 * Что играет главная кнопка: [video] — адрес для плеера, [episode] — серия сериала. У фильма оба
 * null: фильм играется целиком, плеер возьмёт первый трек.
 */
internal data class PlayTarget(val video: VideoRef?, val episode: MediaTrack?)

/**
 * Цель главной кнопки: фильм — целиком; сериал — серия «продолжить», иначе первая серия выбранного
 * сезона, иначе первая вообще. Сериал без серий играть нечем — null, кнопка молчит.
 */
internal fun playTarget(item: Item, series: SeriesData?, seasonEpisodes: List<MediaTrack>): PlayTarget? {
    if (series == null) return PlayTarget(video = null, episode = null)
    val episode = series.resume ?: seasonEpisodes.firstOrNull() ?: item.tracklist.firstOrNull()
    return episode?.let { PlayTarget(it.ref, it) }
}

/** Надзаголовок: «Фильм · Драма», «Сериал · Фантастика». */
internal fun detailsOverline(item: Item): String =
    listOfNotNull(typeLabel(item.type), item.genres.firstOrNull()?.title).joinToString(" · ")

/** Подпись главной кнопки: «Продолжить», если есть с чего, иначе «Смотреть». */
internal fun playTitle(series: SeriesData?): String = if (series?.resume != null) "Продолжить" else "Смотреть"

/**
 * Строка под подписью кнопки — что именно сыграет: у фильма «2 ч 11 мин · 4K», у сериала
 * «S4E7 «Мост» · осталось 29 мин».
 */
internal fun playSubtitle(item: Item, series: SeriesData?, target: PlayTarget?): String? {
    val episode = target?.episode
    if (series == null || episode == null) {
        return listOfNotNull(volumeLabel(item, null), "4K".takeIf { hasUhd(item) }).joinToString(" · ").ifBlank { null }
    }
    val code = if (episode.seasonNumber > 0) "S${episode.seasonNumber}E${episode.number}" else "Серия ${episode.number}"
    val title = episode.title.takeIf { it.isNotBlank() }?.let { "«$it»" }
    val left = remainingMinutes(episode)?.let { "осталось ${durationLabel(it)}" }.takeIf { episode == series.resume }
    return listOfNotNull(listOfNotNull(code, title).joinToString(" "), left).joinToString(" · ")
}

/** Полоска досмотренного под кнопкой — только у серии, которую продолжают. */
internal fun playProgress(series: SeriesData?, target: PlayTarget?): Float? =
    target?.episode?.takeIf { it == series?.resume }?.let(::episodeProgress)?.takeIf { it > 0f }

private fun remainingMinutes(episode: MediaTrack): Int? {
    if (episode.watchedSeconds <= 0) return null
    return ((episode.durationSeconds - episode.watchedSeconds) / SECONDS_IN_MINUTE).takeIf { it > 0 }
}

/** Подписи главной кнопки: что, с какого места и сколько уже досмотрено. */
internal data class PlayLabel(val title: String, val subtitle: String?, val progress: Float?)

internal fun playLabel(item: Item, series: SeriesData?, target: PlayTarget?) = PlayLabel(
    title = playTitle(series),
    subtitle = playSubtitle(item, series, target),
    progress = playProgress(series, target),
)

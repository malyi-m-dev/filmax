// Подписи ТВ-деталей — чистые функции без UI: надзаголовок, оценки, мета, подпись кнопки
// «Смотреть» и модель строки серии. Общие с телефоном правила живут в details.common.
package com.filmax.feature.details.tv

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.tv.designsystem.TvEpisodeRowModel
import com.filmax.core.ui.components.ratingLabel
import com.filmax.core.ui.components.typeLabel
import com.filmax.feature.details.common.SeriesData
import com.filmax.feature.details.common.WATCH_STATUS_FINISHED
import com.filmax.feature.details.common.WATCH_STATUS_IN_PROGRESS
import com.filmax.feature.details.common.episodeProgress
import com.filmax.feature.details.common.episodeRowMeta
import com.filmax.feature.details.common.numberedEpisodeTitle
import com.filmax.feature.details.common.volumeLabel

/** Оценка с источником для бейджа: главный источник выделен. */
internal data class RatingBadge(val value: String, val source: String, val highlighted: Boolean)

/** Сколько жанров влезает в мету без переноса. */
private const val MAX_META_GENRES = 2

/** Надзаголовок: «Фильм · 2022 · Норвегия», «Сериал · 7 сезонов · США». */
internal fun overline(item: Item, series: SeriesData?): String = listOfNotNull(
    typeLabel(item.type),
    if (series == null) item.year.takeIf { it > 0 }?.toString() else volumeLabel(item, series),
    item.country.substringBefore(',').trim().takeIf { it.isNotBlank() },
).joinToString(" · ")

/** Бейджи оценок: Кинопоиск — главный, IMDb — вторым. Ноль — «оценки нет», бейджа нет. */
internal fun ratingBadges(item: Item): List<RatingBadge> = listOfNotNull(
    ratingLabel(item.rating.kinopoisk)?.let { RatingBadge(it, "КиноПоиск", highlighted = true) },
    ratingLabel(item.rating.imdb)?.let { RatingBadge(it, "IMDb", highlighted = false) },
)

/** Мета рядом с оценками: фильм — жанры и длительность, сериал — год и жанры. */
internal fun metaLine(item: Item, series: SeriesData?): String = listOfNotNull(
    item.year.takeIf { series != null && it > 0 }?.toString(),
    item.genres.take(MAX_META_GENRES).joinToString(", ") { it.title }.takeIf { it.isNotBlank() },
    if (series == null) volumeLabel(item, null) else null,
).joinToString(" · ")

/**
 * Мета в панели полного описания: «2022 · Норвегия, Дания · 2 ч 11 мин» /
 * «2012 · США · 7 сезонов, 68 серий». Жанры и оценки остались в шапке под панелью — здесь
 * то, чего в шапке нет: все страны и полный объём сериала.
 */
internal fun sheetMeta(item: Item, series: SeriesData?): String {
    // У сериала — «7 сезонов, 68 серий»; в один сезон — только число серий.
    val volume = if (series == null) {
        volumeLabel(item, null)
    } else {
        val episodes = series.seasons.sumOf { (_, seasonEpisodes) -> seasonEpisodes.size }
        listOfNotNull(
            volumeLabel(item, series).takeIf { series.seasons.size > 1 },
            episodes.takeIf { it > 0 }?.let { "$it ${episodesWord(it)}" },
        ).joinToString(", ").ifBlank { null }
    }
    return listOfNotNull(
        item.year.takeIf { it > 0 }?.toString(),
        item.country.trim().takeIf { it.isNotBlank() },
        volume,
    ).joinToString(" · ")
}

/** «Продолжить · S2E5» — у сериала с недосмотренной серией; иначе «Смотреть». */
internal fun playLabel(resume: MediaTrack?): String = when {
    resume == null -> "Смотреть"
    resume.seasonNumber > 0 -> "Продолжить · S${resume.seasonNumber}E${resume.number}"
    else -> "Продолжить · Серия ${resume.number}"
}

/** Подпись сезона в ряду чипов: выбранный — словом, остальные — номером, чтобы ряд влез. */
internal fun seasonChipLabel(number: Int, selected: Boolean): String = when {
    number <= 0 -> "Серии"
    selected -> "Сезон $number"
    else -> "$number"
}

/** Модель строки серии: «7. Тайный совет», мета о том, где в серии зритель, прогресс. */
internal fun episodeRow(episode: MediaTrack, resumeId: Int?): TvEpisodeRowModel {
    val watched = episode.watchStatus == WATCH_STATUS_FINISHED
    val resume = episode.id == resumeId && !watched
    return TvEpisodeRowModel(
        title = numberedEpisodeTitle(episode),
        meta = episodeRowMeta(episode, resume),
        imageUrl = episode.thumbnail,
        progress = episodeProgress(episode).takeIf { episode.watchStatus == WATCH_STATUS_IN_PROGRESS || watched },
        watched = watched,
        resume = resume,
    )
}

private const val SECONDS_IN_MINUTE = 60

/** «10 серий · ~55 мин» — число серий сезона и средняя длительность; null — сезон пуст. */
internal fun seasonSummary(episodes: List<MediaTrack>): String? {
    if (episodes.isEmpty()) return null
    val durations = episodes.map { it.durationSeconds }.filter { it > 0 }
    val minutes = durations.takeIf { it.isNotEmpty() }?.let { it.average().toInt() / SECONDS_IN_MINUTE }
    return listOfNotNull("${episodes.size} ${episodesWord(episodes.size)}", minutes?.let { "~$it мин" })
        .joinToString(" · ")
}

private fun episodesWord(count: Int): String {
    val lastTwo = count % HUNDRED
    val last = count % TEN
    return when {
        lastTwo in TEENS -> "серий"
        last == 1 -> "серия"
        last in FEW -> "серии"
        else -> "серий"
    }
}

private const val HUNDRED = 100
private const val TEN = 10
private val TEENS = 11..14
private val FEW = 2..4

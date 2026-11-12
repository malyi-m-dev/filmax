// Подписи шапки деталей — готовые строки из состояния, без UI: надзаголовки, бейджи оценок, мета,
// объём сериала, сводка сезона. Раньше их собирали вёрстки телефона и ТВ; телефон и ТВ говорят о
// тайтле по-разному, поэтому строк две там, где разные макеты (как meta и wideMeta у Главной).
package com.filmax.feature.details.common

import com.filmax.core.presentation.format.ratingLabel
import com.filmax.core.presentation.format.title
import com.filmax.core.presentation.format.typeLabel

/** Сколько жанров влезает в мету ТВ без переноса. */
private const val MAX_META_GENRES = 2
private const val SECONDS_IN_MINUTE = 60

/** Оценка с источником для бейджа: главный источник выделен. */
data class RatingBadge(val value: String, val source: String, val highlighted: Boolean)

/** Надзаголовок телефона: «Фильм · Драма», «Сериал · Фантастика». */
val DetailsState.genreOverline: String
    get() = item?.let { item -> listOfNotNull(typeLabel(item.type), item.genres.firstOrNull()?.title) }
        .orEmpty()
        .joinToString(" · ")

/** Надзаголовок ТВ: «Фильм · 2022 · Норвегия», «Сериал · 7 сезонов · США». */
val DetailsState.originOverline: String
    get() {
        val item = item ?: return ""
        return listOfNotNull(
            typeLabel(item.type),
            if (series == null) item.year.takeIf { it > 0 }?.toString() else volumeLabel(item, series),
            item.country.substringBefore(',').trim().takeIf { it.isNotBlank() },
        ).joinToString(" · ")
    }

/**
 * Бейджи оценок в порядке источника: главная (у kino.pub — Кинопоиск) выделена, остальные — за
 * ней. Ноль — «оценки нет», бейджа нет.
 */
val DetailsState.ratingBadges: List<RatingBadge>
    get() = item?.rating?.values.orEmpty().mapIndexedNotNull { index, rating ->
        ratingLabel(rating.value)?.let { RatingBadge(it, rating.source.title, highlighted = index == 0) }
    }

/** Мета рядом с оценками на ТВ: фильм — жанры и длительность, сериал — год и жанры. */
val DetailsState.headingMeta: String
    get() {
        val item = item ?: return ""
        return listOfNotNull(
            item.year.takeIf { series != null && it > 0 }?.toString(),
            item.genres.take(MAX_META_GENRES).joinToString(", ") { it.title }.takeIf { it.isNotBlank() },
            if (series == null) volumeLabel(item, null) else null,
        ).joinToString(" · ")
    }

/**
 * Мета в панели полного описания: «2022 · Норвегия, Дания · 2 ч 11 мин» /
 * «2012 · США · 7 сезонов, 68 серий». Жанры и оценки остались в шапке под панелью — здесь
 * то, чего в шапке нет: все страны и полный объём сериала. null — сказать нечего.
 */
val DetailsState.descriptionMeta: String?
    get() {
        val item = item ?: return null
        val series = series
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
        ).joinToString(" · ").ifBlank { null }
    }

/** «10 серий · ~55 мин» — число серий выбранного сезона и средняя длительность; null — сезон пуст. */
val DetailsState.seasonSummary: String?
    get() {
        val episodes = seasonEpisodes
        if (episodes.isEmpty()) return null
        val durations = episodes.map { it.durationSeconds }.filter { it > 0 }
        val minutes = durations.takeIf { it.isNotEmpty() }?.let { it.average().toInt() / SECONDS_IN_MINUTE }
        return listOfNotNull("${episodes.size} ${episodesWord(episodes.size)}", minutes?.let { "~$it мин" })
            .joinToString(" · ")
    }

/** Подпись сезона в ряду чипов ТВ: выбранный — словом, остальные — номером, чтобы ряд влез. */
fun seasonChipLabel(number: Int, selected: Boolean): String = when {
    number <= 0 -> "Серии"
    selected -> "Сезон $number"
    else -> "$number"
}

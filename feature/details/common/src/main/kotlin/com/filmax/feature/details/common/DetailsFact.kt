package com.filmax.feature.details.common

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.audioLanguageName
import com.filmax.core.domain.catalog.model.subtitleLanguageName

/** Факт в блоке «О фильме»: подпись и значение. */
data class DetailsFact(val label: String, val value: String)

/**
 * Факты «О фильме»: страна, жанр, длительность или объём, языки озвучки и субтитров, лучшее
 * качество. Пустые факты выпадают — пустая клетка в сетке хуже её отсутствия. Языки и качество —
 * по первой дорожке: у kino.pub они общие для всех серий.
 */
fun detailsFacts(item: Item, series: SeriesData?): List<DetailsFact> {
    val track = item.tracklist.firstOrNull()
    return listOfNotNull(
        item.country.takeIf { it.isNotBlank() }?.let { DetailsFact("Страна", it) },
        item.genres.takeIf { it.isNotEmpty() }
            ?.let { genres -> DetailsFact("Жанр", genres.joinToString(", ") { it.title }) },
        volumeLabel(item, series)?.let { DetailsFact(if (series == null) "Длительность" else "Объём", it) },
        track?.audios?.map { audioLanguageName(it.lang) }?.distinct()?.takeIf { it.isNotEmpty() }
            ?.let { DetailsFact("Аудио", it.joinToString(", ")) },
        track?.subtitles?.map { subtitleLanguageName(it.lang) }?.distinct()?.takeIf { it.isNotEmpty() }
            ?.let { DetailsFact("Субтитры", it.joinToString(", ")) },
        track?.files?.maxByOrNull { qualityRank(it.quality) }?.quality?.let { DetailsFact("Качество", "до $it") },
    )
}

/** Вес качества для выбора лучшего: «2160p» > «1080p» > «720p»; «4K» — как 2160. */
private fun qualityRank(quality: String): Int = when {
    quality.contains("4k", ignoreCase = true) -> UHD_LINES
    else -> quality.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
}

private const val UHD_LINES = 2160

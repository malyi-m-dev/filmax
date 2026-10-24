// Подписи «Моё» на телефоне — чистые функции без UI: что смотрели и когда, подпись папки.

package com.filmax.feature.library.mobile

import com.filmax.core.domain.watching.model.WatchProgress

/** С какой доли тайтл в истории считается досмотренным. */
private const val WATCHED_FRACTION = 0.95f
private const val PERCENT = 100

/** Что смотрели: «S4E7» у сериала, «Фильм» — когда серии нет. */
internal fun historySubtitle(progress: WatchProgress?): String {
    val season = progress?.season?.takeIf { it > 0 }
    val number = progress?.videoId?.takeIf { it > 0 }
    return if (season != null && number != null) "S${season}E$number" else "Фильм"
}

/** Когда и сколько: «21:10 · на 45%», досмотренное — «просмотрено». */
internal fun historyMeta(progress: WatchProgress?, time: String?): String? {
    val fraction = progress?.fraction ?: 0f
    val amount = when {
        fraction >= WATCHED_FRACTION -> "просмотрено"
        fraction > 0f -> "на ${(fraction * PERCENT).toInt()}%"
        else -> null
    }
    return listOfNotNull(time, amount).joinToString(" · ").ifBlank { null }
}

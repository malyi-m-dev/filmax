// Подписи прогресса просмотра — что смотрели и сколько осталось. Общие для «Продолжить» и
// «Истории» на телефоне и ТВ; раньше жили копиями в UI-модулях каждой платформы.

package com.filmax.core.presentation.labels

import com.filmax.core.domain.watching.model.WatchProgress

/** С какой доли тайтл считается досмотренным — хвост с титрами за просмотр не считаем. */
private const val WATCHED_FRACTION = 0.95f
private const val PERCENT = 100
private const val SECONDS_IN_MINUTE = 60

/** Серия из прогресса: «S4E7». Фильм или неизвестная серия — null. */
fun episodeCode(progress: WatchProgress?): String? {
    val season = progress?.season?.takeIf { it > 0 }
    val number = progress?.videoId?.takeIf { it > 0 }
    return if (season != null && number != null) "S${season}E$number" else null
}

/** «осталось 41 мин»; остатка нет — «просмотрено»; времени нет — null. */
fun remainingLabel(progress: WatchProgress?): String? {
    val watched = progress?.timeSeconds
    val total = progress?.durationSeconds?.takeIf { it > 0 }
    if (watched == null || total == null) return null
    val minutes = (total - watched) / SECONDS_IN_MINUTE
    return if (minutes > 0) "осталось ${durationLabel(minutes)}" else "просмотрено"
}

/** Сколько посмотрели: «на 45%», досмотренное — «просмотрено», нетронутое — null. */
fun watchedShare(progress: WatchProgress?): String? {
    val fraction = progress?.fraction ?: 0f
    return when {
        fraction >= WATCHED_FRACTION -> "просмотрено"
        fraction > 0f -> "на ${(fraction * PERCENT).toInt()}%"
        else -> null
    }
}

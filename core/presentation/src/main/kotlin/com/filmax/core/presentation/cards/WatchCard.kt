// Карточки просмотра и «Буду смотреть» с готовыми подписями — их рисуют «Продолжить» Главной и
// «Моё» на телефоне и ТВ. Подписи считает модель, а не вёрстка: телефон и ТВ берут одну карточку,
// а тесты модели видят ровно то, что на экране.
package com.filmax.core.presentation.cards

import com.filmax.core.domain.catalog.model.VideoRef
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.domain.watching.model.WatchProgress
import com.filmax.core.domain.watchlist.model.WatchlistItem
import com.filmax.core.presentation.format.continueMeta
import com.filmax.core.presentation.format.durationLabel
import com.filmax.core.presentation.format.isHighRating
import com.filmax.core.presentation.format.ratingLabel

/** С какой доли тайтл в истории считается досмотренным. */
private const val WATCHED_FRACTION = 0.95f
private const val PERCENT = 100
private const val SECONDS_IN_MINUTE = 60

/**
 * Карточка просмотра — «Продолжить» и история. Подписей несколько, потому что телефон и ТВ
 * показывают о просмотре разное: телефон — когда и сколько, ТВ — какая серия и сколько осталось.
 */
data class WatchCard(
    val itemId: Int,
    val title: String,
    /** Кадр серии, иначе широкий постер, иначе обложка. */
    val imageUrl: String,
    val progress: Float,
    /** Куда вести плеер: серия с сезоном или фильм целиком. */
    val video: VideoRef?,
    /** Плашка серии на кадре: «S4E7»; у фильма — null. */
    val episode: String?,
    /** Что смотрели: «S4E7» или «Фильм» — строка истории на телефоне. */
    val whatWatched: String,
    /** «S2 · осталось 18 мин» — карточка «Продолжить» на телефоне. */
    val continueMeta: String?,
    /** «осталось 41 мин» или «просмотрено» — карточка «Продолжить» на ТВ. */
    val remaining: String?,
    /** Когда смотрели: «21:40»; null — сервер не прислал время. */
    val time: String?,
    /** Когда и сколько: «21:10 · на 45%» — строка истории на телефоне. */
    val whenWatched: String?,
    /** Что и когда: «S4E7 · 21:40» — карточка истории на ТВ. */
    val episodeAndTime: String?,
)

/** Постер «Буду смотреть»: оценка уже подписью, высокая — выделяется вторым акцентом. */
data class WatchlistCard(
    val id: Int,
    val title: String,
    val posterUrl: String,
    val rating: String?,
    val ratingHigh: Boolean,
    /** Год под постером; null — неизвестен. */
    val year: String?,
)

fun watchCard(entry: WatchHistory, time: String? = null): WatchCard {
    val progress = entry.progress
    val episode = episodeCode(progress)
    return WatchCard(
        itemId = entry.itemId,
        title = entry.title,
        imageUrl = entry.wideOrPoster,
        progress = progress?.fraction ?: 0f,
        video = progress?.video,
        episode = episode,
        whatWatched = episode ?: "Фильм",
        continueMeta = continueMeta(progress),
        remaining = remainingLabel(progress),
        time = time,
        whenWatched = listOfNotNull(time, watchedShare(progress)).joinToString(" · ").ifBlank { null },
        episodeAndTime = listOfNotNull(episode, time).joinToString(" · ").ifBlank { null },
    )
}

fun watchlistCard(item: WatchlistItem): WatchlistCard = WatchlistCard(
    id = item.id,
    title = item.title,
    posterUrl = item.posterSmall,
    rating = ratingLabel(item.rating),
    ratingHigh = isHighRating(item.rating),
    year = item.year.takeIf { it > 0 }?.toString(),
)

/** «S4E7» у серии; фильм или неизвестная серия — null. */
private fun episodeCode(progress: WatchProgress?): String? {
    val season = progress?.season?.takeIf { it > 0 }
    val number = progress?.videoId?.takeIf { it > 0 }
    return if (season != null && number != null) "S${season}E$number" else null
}

/** «осталось 41 мин»; остатка нет — «просмотрено»; прогресса нет — null. */
private fun remainingLabel(progress: WatchProgress?): String? {
    val watched = progress?.timeSeconds
    val total = progress?.durationSeconds?.takeIf { it > 0 }
    if (watched == null || total == null) return null
    val minutes = (total - watched) / SECONDS_IN_MINUTE
    return if (minutes > 0) "осталось ${durationLabel(minutes)}" else "просмотрено"
}

/** «на 45%», досмотренное — «просмотрено», не начатое — null. */
private fun watchedShare(progress: WatchProgress?): String? {
    val fraction = progress?.fraction ?: 0f
    return when {
        fraction >= WATCHED_FRACTION -> "просмотрено"
        fraction > 0f -> "на ${(fraction * PERCENT).toInt()}%"
        else -> null
    }
}

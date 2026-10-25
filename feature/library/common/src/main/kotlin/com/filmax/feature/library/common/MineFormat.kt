// Правила раздела «Моё» без UI: что считается «начатым», как история делится на дни, как
// сортируются «Буду смотреть» и папки, как подписать дату. Чистые функции — их зовёт
// MineScreenModel и проверяют тесты с фиксированным «сейчас».
package com.filmax.feature.library.common

import com.filmax.core.domain.favorites.model.FavoriteItem
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.presentation.labels.episodeCode
import com.filmax.core.presentation.labels.watchedShare
import java.text.Collator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Доля просмотра, при которой тайтл считается начатым, но не досмотренным. */
private const val CONTINUE_MIN_FRACTION = 0.01f
private const val CONTINUE_MAX_FRACTION = 0.95f

private val Russian = Locale.forLanguageTag("ru")
private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm", Russian)

/** «12 октября» — месяц в родительном падеже: так его отдаёт русская локаль в формате даты. */
private val DayFormat = DateTimeFormatter.ofPattern("d MMMM", Russian)
private val DayWithYearFormat = DateTimeFormatter.ofPattern("d MMMM yyyy", Russian)

/** Сравнение названий по-русски: «ё» рядом с «е», регистр не важен. */
private val TitleCollator: Collator = Collator.getInstance(Russian).apply { strength = Collator.SECONDARY }

/** Начатое и не досмотренное — то, что уходит в «Продолжить». */
fun continueEntries(history: List<WatchHistory>): List<WatchHistory> = history.filter { entry ->
    val fraction = entry.progress?.fraction ?: 0f
    fraction > CONTINUE_MIN_FRACTION && fraction < CONTINUE_MAX_FRACTION
}

/**
 * История по дням просмотра, свежие дни первыми. Внутри дня — порядок сервера (тоже от свежего).
 * Записи без времени (сервер его не прислал) собираются в «Раньше» в самом конце.
 */
fun historyDays(history: List<WatchHistory>, now: ZonedDateTime): List<HistoryDay> {
    val zone = now.zone
    return history
        .groupBy { entry -> entry.lastSeenAt?.let { localDate(it, zone) } }
        .entries
        .sortedByDescending { (day, _) -> day ?: LocalDate.MIN }
        .map { (day, entries) -> HistoryDay(dayLabel(day, now.toLocalDate()), entries.map { historyEntry(it, zone) }) }
}

private fun historyEntry(entry: WatchHistory, zone: ZoneId) = HistoryEntry(
    entry = entry,
    time = entry.lastSeenAt?.let { millis -> timeLabel(millis, zone) },
    episodeCode = episodeCode(entry.progress),
    watchedShare = watchedShare(entry.progress),
)

/** «Сегодня», «Вчера», «12 октября», в прошлые годы — с годом; null — «Раньше». */
fun dayLabel(day: LocalDate?, today: LocalDate): String = when {
    day == null -> "Раньше"
    day == today -> "Сегодня"
    day == today.minusDays(1) -> "Вчера"
    day.year == today.year -> day.format(DayFormat)
    else -> day.format(DayWithYearFormat)
}

/** Подпись изменения папки: «изменена сегодня», «изменена вчера», «изменена 12 октября». */
fun changedLabel(updatedAt: Long?, now: ZonedDateTime): String? {
    val day = updatedAt?.let { localDate(it, now.zone) } ?: return null
    return "изменена ${dayLabel(day, now.toLocalDate()).replaceFirstChar { it.lowercase(Russian) }}"
}

fun sortedWatchlist(items: List<FavoriteItem>, sort: WatchlistSort): List<FavoriteItem> = when (sort) {
    WatchlistSort.NEWEST -> items
    // Без оценки — в конец: «по рейтингу» значит «сначала лучшее из известного».
    WatchlistSort.RATING -> items.sortedByDescending { it.rating?.toDoubleOrNull() ?: Double.NEGATIVE_INFINITY }
    WatchlistSort.TITLE -> items.sortedWith(compareBy(TitleCollator) { it.title })
}

fun sortedFolders(folders: List<MineFolder>, sort: FolderSort): List<MineFolder> = when (sort) {
    // Только что созданная папка приходит без даты — она самая свежая и встаёт первой.
    FolderSort.UPDATED -> folders.sortedByDescending { it.folder.updatedAt ?: Long.MAX_VALUE }
    FolderSort.TITLE -> folders.sortedWith(compareBy(TitleCollator) { it.folder.title })
}

/** Следующее значение перечисления по кругу — для чипов сортировки. */
inline fun <reified T : Enum<T>> T.next(): T = enumValues<T>().let { values -> values[(ordinal + 1) % values.size] }

private fun localDate(millis: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

private fun timeLabel(millis: Long, zone: ZoneId): String = Instant.ofEpochMilli(millis).atZone(zone).format(TimeFormat)

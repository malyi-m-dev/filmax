// Неизменные образцы состояния «Моего» — для превью телефона и ТВ. Собраны теми же функциями, что
// и в модели (watchCard, watchlistCard), поэтому превью показывает настоящие подписи.
package com.filmax.feature.library.common

import com.filmax.core.domain.user.model.UserFolder
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.domain.watching.model.WatchProgress
import com.filmax.core.domain.watchlist.model.WatchlistItem
import com.filmax.core.presentation.cards.watchCard
import com.filmax.core.presentation.cards.watchlistCard

private const val SERIES_DURATION_SECONDS = 3_120
private const val MOVIE_DURATION_SECONDS = 7_860
private const val STATUS_IN_PROGRESS = 0

object MineSamples {

    private val series = WatchHistory(
        itemId = 1,
        title = "Хроники Пограничья",
        posterSmall = null,
        progress = WatchProgress(STATUS_IN_PROGRESS, 1_260, SERIES_DURATION_SECONDS, videoId = 7, season = 4),
        lastSeenAt = null,
    )

    private val movie = WatchHistory(
        itemId = 2,
        title = "Дом на краю карты",
        posterSmall = null,
        progress = WatchProgress(STATUS_IN_PROGRESS, 5_400, MOVIE_DURATION_SECONDS, videoId = 1, season = null),
        lastSeenAt = null,
    )

    private val watchlist = listOf(
        WatchlistItem(id = 3, title = "Северный ветер", posterSmall = "", year = 2023, durationMinutes = 48, 8.4),
        WatchlistItem(id = 4, title = "Код доступа", posterSmall = "", year = 2021, durationMinutes = 112, 7.1),
        WatchlistItem(id = 5, title = "Полярный круг", posterSmall = "", year = 2019, durationMinutes = 95),
    ).map(::watchlistCard)

    private val folders = listOf(
        MineFolder(
            folder = UserFolder(id = 2, title = "Пересмотреть с семьёй", count = 4, updatedAt = null, tint = 1),
            posters = emptyList(),
            changed = "изменена вчера",
            count = "4 тайтла",
            caption = "4 тайтла · изменена вчера",
        ),
        MineFolder(
            folder = UserFolder(id = 3, title = "Отложено на потом", count = 25, updatedAt = null, tint = 3),
            posters = emptyList(),
            changed = null,
            count = "25 тайтлов",
            caption = "25 тайтлов",
        ),
    )

    /** Всё загружено, открыт «Продолжить». */
    val loaded = MineState(
        loading = false,
        continueWatching = listOf(watchCard(series), watchCard(movie)),
        history = listOf(
            HistoryDay("Сегодня", listOf(watchCard(series, time = "21:40"))),
            HistoryDay("Вчера", listOf(watchCard(movie, time = "19:05"))),
        ),
        watchlist = watchlist,
        folders = folders,
    )

    val watchlistSegment = loaded.copy(segment = MineSegment.WATCHLIST)
    val foldersSegment = loaded.copy(segment = MineSegment.FOLDERS)
    val historySegment = loaded.copy(segment = MineSegment.HISTORY)

    /** Домашний сервер: папок нет — и сегмента нет. */
    val withoutFolders = loaded.copy(segments = MineSegment.entries - MineSegment.FOLDERS)

    val empty = MineState(loading = false)
    val loading = MineState()

    val all: List<MineState> =
        listOf(loaded, watchlistSegment, foldersSegment, historySegment, withoutFolders, empty, loading)
}

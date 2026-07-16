package com.filmax.data.watching

import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.common.safeRequest
import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.core.domain.watching.model.Notification
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.domain.watching.model.WatchProgress
import com.filmax.data.watching.remote.WatchingApi
import com.filmax.data.watching.remote.dto.HistoryItemDto
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** Единственные типы, которые принимает `watching/{type}`. «all» и прочее дают пустой список. */
private val HISTORY_TYPES = listOf("movies", "serials")

private fun HistoryItemDto.toDomain() = WatchHistory(
    itemId = id,
    title = title,
    posterSmall = posters?.small,
    posterWide = posters?.wide,
    progress = watching?.let {
        WatchProgress(
            status = it.status,
            timeSeconds = it.time,
            durationSeconds = it.duration,
            videoId = it.video,
            season = it.season,
        )
    },
)

internal class WatchingRepositoryImpl(
    private val api: WatchingApi,
) : WatchingRepository {

    /**
     * Начатое = фильмы + сериалы двумя запросами. Одного «все» у kino.pub не существует: тип в
     * `watching/{type}` — это ровно `movies` либо `serials`, и на любом другом значении список
     * приходит пустым (так и жила вечно пустая история).
     *
     * Запросы параллельны: ждать их последовательно — удваивать задержку раздела на ровном месте.
     */
    override suspend fun getHistory(type: String): RequestResult<List<WatchHistory>> = safeRequest {
        coroutineScope {
            HISTORY_TYPES
                .map { historyType -> async { api.getHistory(historyType).items } }
                .awaitAll()
                .flatten()
                .map { dto -> dto.toDomain() }
        }
    }

    override suspend fun saveProgress(itemId: Int, videoId: Int, timeSeconds: Int): RequestResult<Unit> =
        safeRequest { api.saveProgress(itemId, videoId, timeSeconds) }

    override suspend fun saveProgressSerial(
        itemId: Int,
        season: Int,
        videoId: Int,
        timeSeconds: Int,
    ): RequestResult<Unit> =
        safeRequest { api.saveProgressSerial(itemId, season, videoId, timeSeconds) }

    override suspend fun toggleWatched(itemId: Int): RequestResult<Unit> =
        safeRequest { api.toggleWatched(itemId) }

    override suspend fun toggleWatchlist(itemId: Int): RequestResult<Boolean> =
        safeRequest { api.toggleWatchlist(itemId)["watching"] == 1 }

    override suspend fun clearHistory(itemId: Int): RequestResult<Unit> =
        safeRequest { api.clearItemHistory(itemId) }

    override suspend fun getNotifications(): RequestResult<List<Notification>> = safeRequest {
        api.getNotifications().notifications?.map { dto ->
            Notification(
                id = dto.id,
                title = dto.title,
                text = dto.text,
                createdAt = dto.createdAt?.toLong()?.times(MILLIS_IN_SECOND),
                read = dto.read,
                itemId = dto.itemId,
            )
        } ?: emptyList()
    }

    override suspend fun markNotificationRead(id: Int): RequestResult<Unit> =
        safeRequest { api.markNotificationRead(id) }

    override suspend fun markAllNotificationsRead(): RequestResult<Unit> =
        safeRequest { api.markAllNotificationsRead() }

    private companion object {
        // kino.pub отдаёт временные метки в секундах — переводим в миллисекунды.
        const val MILLIS_IN_SECOND = 1000
    }
}

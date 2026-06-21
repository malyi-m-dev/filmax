package com.filmax.data.watching

import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.common.safeRequest
import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.core.domain.watching.model.Notification
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.domain.watching.model.WatchProgress
import com.filmax.data.watching.remote.WatchingApi

internal class WatchingRepositoryImpl(
    private val api: WatchingApi,
) : WatchingRepository {

    override suspend fun getHistory(type: String): RequestResult<List<WatchHistory>> = safeRequest {
        api.getHistory(type).items.map { dto ->
            WatchHistory(
                itemId = dto.id,
                title = dto.title,
                posterSmall = dto.posters?.small,
                progress = dto.watching?.let {
                    WatchProgress(
                        status = it.status,
                        timeSeconds = it.time,
                        durationSeconds = it.duration,
                        videoId = it.video,
                        season = it.season,
                    )
                },
            )
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
                createdAt = dto.createdAt?.toLong()?.times(1000),
                read = dto.read,
                itemId = dto.itemId,
            )
        } ?: emptyList()
    }

    override suspend fun markNotificationRead(id: Int): RequestResult<Unit> =
        safeRequest { api.markNotificationRead(id) }

    override suspend fun markAllNotificationsRead(): RequestResult<Unit> =
        safeRequest { api.markAllNotificationsRead() }
}

package com.filmax.data.watching

import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.core.domain.watching.model.Notification
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.domain.watching.model.WatchProgress
import com.filmax.data.watching.remote.WatchingApi
import javax.inject.Inject

internal class WatchingRepositoryImpl @Inject constructor(
    private val api: WatchingApi,
) : WatchingRepository {

    override suspend fun getHistory(type: String): List<WatchHistory> =
        api.getHistory(type).items.map { dto ->
            WatchHistory(
                itemId      = dto.id,
                title       = dto.title,
                posterSmall = dto.posters?.small,
                progress    = dto.watching?.let {
                    WatchProgress(
                        status          = it.status,
                        timeSeconds     = it.time,
                        durationSeconds = it.duration,
                        videoId         = it.video,
                        season          = it.season,
                    )
                },
            )
        }

    override suspend fun saveProgress(itemId: Int, videoId: Int, timeSeconds: Int) {
        api.saveProgress(itemId, videoId, timeSeconds)
    }

    override suspend fun saveProgressSerial(
        itemId: Int,
        season: Int,
        videoId: Int,
        timeSeconds: Int,
    ) {
        api.saveProgressSerial(itemId, season, videoId, timeSeconds)
    }

    override suspend fun toggleWatched(itemId: Int) {
        api.toggleWatched(itemId)
    }

    override suspend fun toggleWatchlist(itemId: Int): Boolean {
        val result = api.toggleWatchlist(itemId)
        return result["watching"] == 1
    }

    override suspend fun clearHistory(itemId: Int) {
        api.clearItemHistory(itemId)
    }

    override suspend fun getNotifications(): List<Notification> =
        api.getNotifications().notifications?.map { dto ->
            Notification(
                id        = dto.id,
                title     = dto.title,
                text      = dto.text,
                createdAt = dto.createdAt?.toLong()?.times(1000),
                read      = dto.read,
                itemId    = dto.itemId,
            )
        } ?: emptyList()

    override suspend fun markNotificationRead(id: Int) {
        api.markNotificationRead(id)
    }

    override suspend fun markAllNotificationsRead() {
        api.markAllNotificationsRead()
    }
}

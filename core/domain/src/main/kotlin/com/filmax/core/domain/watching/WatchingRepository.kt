package com.filmax.core.domain.watching

import com.filmax.core.domain.watching.model.Notification
import com.filmax.core.domain.watching.model.WatchHistory
import kotlinx.coroutines.flow.Flow

interface WatchingRepository {

    suspend fun getHistory(type: String = "all"): List<WatchHistory>

    suspend fun saveProgress(itemId: Int, videoId: Int, timeSeconds: Int)

    suspend fun saveProgressSerial(
        itemId: Int,
        season: Int,
        videoId: Int,
        timeSeconds: Int,
    )

    suspend fun toggleWatched(itemId: Int)

    suspend fun toggleWatchlist(itemId: Int): Boolean

    suspend fun clearHistory(itemId: Int)

    suspend fun getNotifications(): List<Notification>

    suspend fun markNotificationRead(id: Int)

    suspend fun markAllNotificationsRead()
}

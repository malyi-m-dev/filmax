package com.filmax.core.domain.watching

import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.watching.model.Notification
import com.filmax.core.domain.watching.model.WatchHistory

interface WatchingRepository {

    suspend fun getHistory(type: String = "all"): RequestResult<List<WatchHistory>>

    suspend fun saveProgress(itemId: Int, videoId: Int, timeSeconds: Int): RequestResult<Unit>

    suspend fun saveProgressSerial(
        itemId: Int,
        season: Int,
        videoId: Int,
        timeSeconds: Int,
    ): RequestResult<Unit>

    suspend fun toggleWatched(itemId: Int): RequestResult<Unit>

    suspend fun toggleWatchlist(itemId: Int): RequestResult<Boolean>

    suspend fun clearHistory(itemId: Int): RequestResult<Unit>

    suspend fun getNotifications(): RequestResult<List<Notification>>

    suspend fun markNotificationRead(id: Int): RequestResult<Unit>

    suspend fun markAllNotificationsRead(): RequestResult<Unit>
}

package com.filmax.core.domain.watching

import com.filmax.core.domain.catalog.model.VideoRef
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.watching.model.WatchHistory

interface WatchingRepository {

    suspend fun getHistory(type: String = "all"): RequestResult<List<WatchHistory>>

    /** Позиция [timeSeconds] видео [video] тайтла [itemId]: серия — с сезоном, фильм — по номеру. */
    suspend fun saveProgress(itemId: Int, video: VideoRef, timeSeconds: Int): RequestResult<Unit>

    suspend fun toggleWatched(itemId: Int): RequestResult<Unit>

    suspend fun clearHistory(itemId: Int): RequestResult<Unit>
}

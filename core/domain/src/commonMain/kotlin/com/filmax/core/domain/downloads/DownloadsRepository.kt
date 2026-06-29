package com.filmax.core.domain.downloads

import com.filmax.core.domain.downloads.model.DownloadedItem
import kotlinx.coroutines.flow.Flow

/** Хранилище скачанных фильмов (метаданные сохраняются локально). */
interface DownloadsRepository {
    val downloads: Flow<List<DownloadedItem>>

    fun isDownloaded(id: Int): Flow<Boolean>

    suspend fun add(item: DownloadedItem)

    suspend fun remove(id: Int)
}

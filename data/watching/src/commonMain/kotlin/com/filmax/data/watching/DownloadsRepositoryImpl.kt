package com.filmax.data.watching

import com.filmax.core.domain.downloads.DownloadsRepository
import com.filmax.core.domain.downloads.model.DownloadedItem
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Хранилище скачанных фильмов на multiplatform-settings (метаданные в JSON).
 * Реактивность — через [MutableStateFlow], сидируется из персистентного хранилища.
 */
internal class DownloadsRepositoryImpl(
    private val settings: Settings,
) : DownloadsRepository {

    private val state = MutableStateFlow(load())

    override val downloads: Flow<List<DownloadedItem>> = state.asStateFlow()

    override fun isDownloaded(id: Int): Flow<Boolean> =
        state.map { list -> list.any { it.id == id } }

    override suspend fun add(item: DownloadedItem) {
        val updated = state.value.filterNot { it.id == item.id } + item
        persist(updated)
    }

    override suspend fun remove(id: Int) {
        persist(state.value.filterNot { it.id == id })
    }

    private fun persist(list: List<DownloadedItem>) {
        settings.putString(KEY, json.encodeToString(list.map { it.toStored() }))
        state.value = list
    }

    private fun load(): List<DownloadedItem> =
        settings.getStringOrNull(KEY)
            ?.let { runCatching { json.decodeFromString<List<Stored>>(it) }.getOrNull() }
            ?.map { it.toModel() }
            ?: emptyList()

    @Serializable
    private data class Stored(
        val id: Int,
        val title: String,
        val posterSmall: String,
        val year: Int,
        val durationMinutes: Int,
    )

    private fun DownloadedItem.toStored() = Stored(id, title, posterSmall, year, durationMinutes)
    private fun Stored.toModel() = DownloadedItem(id, title, posterSmall, year, durationMinutes)

    private companion object {
        const val KEY = "downloaded_items"
        val json = Json { ignoreUnknownKeys = true }
    }
}

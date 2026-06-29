package com.filmax.data.watching

import com.filmax.core.domain.favorites.FavoritesRepository
import com.filmax.core.domain.favorites.model.FavoriteItem
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
 * Локальное хранилище избранного на multiplatform-settings (метаданные в JSON).
 * Зеркалит подход [DownloadsRepositoryImpl]; сервер синхронизируется отдельно
 * через watchlist-тоггл на стороне ScreenModel.
 */
internal class FavoritesRepositoryImpl(
    private val settings: Settings,
) : FavoritesRepository {

    private val state = MutableStateFlow(load())

    override val favorites: Flow<List<FavoriteItem>> = state.asStateFlow()

    override val favoriteIds: Flow<Set<Int>> = state.map { list -> list.map { it.id }.toSet() }

    override fun isFavorite(id: Int): Flow<Boolean> =
        state.map { list -> list.any { it.id == id } }

    override suspend fun toggle(item: FavoriteItem): Boolean =
        if (state.value.any { it.id == item.id }) {
            remove(item.id)
            false
        } else {
            add(item)
            true
        }

    override suspend fun add(item: FavoriteItem) {
        val updated = state.value.filterNot { it.id == item.id } + item
        persist(updated)
    }

    override suspend fun remove(id: Int) {
        persist(state.value.filterNot { it.id == id })
    }

    private fun persist(list: List<FavoriteItem>) {
        settings.putString(KEY, json.encodeToString(list.map { it.toStored() }))
        state.value = list
    }

    private fun load(): List<FavoriteItem> =
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

    private fun FavoriteItem.toStored() = Stored(id, title, posterSmall, year, durationMinutes)
    private fun Stored.toModel() = FavoriteItem(id, title, posterSmall, year, durationMinutes)

    private companion object {
        const val KEY = "favorite_items"
        val json = Json { ignoreUnknownKeys = true }
    }
}

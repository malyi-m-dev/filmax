package com.filmax.data.fake

import com.filmax.core.domain.downloads.DownloadsRepository
import com.filmax.core.domain.downloads.model.DownloadedItem
import com.filmax.core.domain.favorites.FavoritesRepository
import com.filmax.core.domain.favorites.model.FavoriteItem
import com.filmax.core.domain.user.FolderAppearanceRepository
import com.filmax.core.domain.user.model.FolderAppearance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

// Локальные хранилища приложения в памяти. Настоящие живут в multiplatform-settings устройства и
// к kino.pub не ходят, поэтому мок-сборка берёт их как есть, а юнит-тестам ScreenModel'ей нужны
// эти: чистое состояние на каждый тест, без Android-контекста.

/** «Буду смотреть» в памяти — вместо локального кэша избранного. */
class FakeFavoritesRepository : FavoritesRepository {
    private val items = MutableStateFlow<List<FavoriteItem>>(emptyList())

    override val favorites: Flow<List<FavoriteItem>> = items
    override val favoriteIds: Flow<Set<Int>> = items.map { list -> list.mapTo(HashSet()) { it.id } }

    override fun isFavorite(id: Int): Flow<Boolean> = items.map { list -> list.any { it.id == id } }

    override suspend fun toggle(item: FavoriteItem): Boolean {
        val present = items.value.any { it.id == item.id }
        items.value = if (present) items.value.filterNot { it.id == item.id } else items.value + item
        return !present
    }

    override suspend fun add(item: FavoriteItem) {
        if (items.value.none { it.id == item.id }) items.value = items.value + item
    }

    override suspend fun remove(id: Int) {
        items.value = items.value.filterNot { it.id == id }
    }
}

/** Оформление папок в памяти — вместо записи в настройках устройства. */
class FakeFolderAppearanceRepository : FolderAppearanceRepository {
    private val state = MutableStateFlow<Map<Int, FolderAppearance>>(emptyMap())

    override val appearances: Flow<Map<Int, FolderAppearance>> = state

    override suspend fun setTint(folderId: Int, tint: Int) = edit(folderId) { it.copy(tint = tint) }

    override suspend fun rename(folderId: Int, title: String) = edit(folderId) { it.copy(title = title) }

    override suspend fun forget(folderId: Int) {
        state.update { it - folderId }
    }

    private fun edit(folderId: Int, transform: (FolderAppearance) -> FolderAppearance) {
        state.update { current -> current + (folderId to transform(current[folderId] ?: FolderAppearance())) }
    }
}

/** Загрузки в памяти — вместо списка скачанного на устройстве. */
class FakeDownloadsRepository : DownloadsRepository {
    private val items = MutableStateFlow<List<DownloadedItem>>(emptyList())

    override val downloads: Flow<List<DownloadedItem>> = items

    override fun isDownloaded(id: Int): Flow<Boolean> = items.map { list -> list.any { it.id == id } }

    override suspend fun add(item: DownloadedItem) {
        if (items.value.none { it.id == item.id }) items.value = items.value + item
    }

    override suspend fun remove(id: Int) {
        items.value = items.value.filterNot { it.id == id }
    }
}

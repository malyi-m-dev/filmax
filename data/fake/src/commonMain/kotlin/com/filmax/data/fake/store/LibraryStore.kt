package com.filmax.data.fake.store

import com.filmax.data.fake.FakeIds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.updateAndGet

/** Папка-закладка: тайтлы — свежедобавленные первыми, как в `bookmarks/{id}`. */
internal data class FolderState(
    val id: Int,
    val title: String,
    val itemIds: List<Int>,
    val updatedAt: Long,
)

/**
 * Папки и флаг watchlist. Флаг (`togglewatchlist`, поле `in_watchlist`) у kino.pub не связан с
 * папкой «Буду смотреть»: это две разные сущности, склеивает их приложение. Фейк повторяет сервер
 * честно — иначе он прятал бы расхождения, которые на живом API видны.
 */
internal data class LibrarySnapshot(
    val folders: List<FolderState>,
    val watchlist: Set<Int>,
)

/**
 * Закладки на сессию. Состояние неизменяемое и меняется атомарно через StateFlow: «Буду смотреть»
 * правит FavoritesRepositoryImpl со своего Default-скоупа, а экраны — с главного потока.
 */
internal class LibraryStore(
    initialFolders: List<FolderState>,
    private val clock: () -> Long,
) {
    // Флаг watchlist на старте совпадает с содержимым «Буду смотреть»: иначе первое же открытие
    // деталей «досинхронизировало» бы в папку тайтлы, которых зритель туда не клал.
    private val state = MutableStateFlow(
        LibrarySnapshot(
            folders = initialFolders,
            watchlist = initialFolders.firstOrNull { it.id == FakeIds.WATCHLIST_FOLDER }
                ?.itemIds.orEmpty()
                .toSet(),
        ),
    )

    fun folders(): List<FolderState> = state.value.folders

    fun folder(id: Int): FolderState? = state.value.folders.firstOrNull { it.id == id }

    /** Новая папка — в конец списка; id выдаёт «сервер», следующий за самым большим. */
    fun createFolder(title: String): FolderState {
        val now = clock()
        val snapshot = state.updateAndGet { current ->
            val nextId = (current.folders.maxOfOrNull { it.id } ?: 0) + 1
            current.copy(folders = current.folders + FolderState(nextId, title, emptyList(), now))
        }
        return snapshot.folders.last()
    }

    /** false — такой папки нет (сервер ответил бы 404). */
    fun deleteFolder(id: Int): Boolean {
        val before = state.getAndUpdate { current ->
            current.copy(folders = current.folders.filterNot { it.id == id })
        }
        return before.hasFolder(id)
    }

    /** Добавленный тайтл встаёт первым; повторное добавление лишь поднимает его наверх. */
    fun addItem(folderId: Int, itemId: Int): Boolean =
        editFolder(folderId) { itemIds -> listOf(itemId) + itemIds.filterNot { it == itemId } }

    fun removeItem(folderId: Int, itemId: Int): Boolean =
        editFolder(folderId) { itemIds -> itemIds.filterNot { it == itemId } }

    fun isInWatchlist(itemId: Int): Boolean = itemId in state.value.watchlist

    /** @return новое состояние флага: true — тайтл теперь в watchlist. */
    fun toggleWatchlist(itemId: Int): Boolean {
        val snapshot = state.updateAndGet { current ->
            val watchlist = current.watchlist
            current.copy(watchlist = if (itemId in watchlist) watchlist - itemId else watchlist + itemId)
        }
        return itemId in snapshot.watchlist
    }

    /** Правит состав папки и дату её изменения. false — папки нет. */
    private fun editFolder(folderId: Int, edit: (List<Int>) -> List<Int>): Boolean {
        val now = clock()
        val before = state.getAndUpdate { current ->
            current.copy(
                folders = current.folders.map { folder ->
                    if (folder.id != folderId) return@map folder
                    folder.copy(itemIds = edit(folder.itemIds), updatedAt = now)
                },
            )
        }
        return before.hasFolder(folderId)
    }
}

private fun LibrarySnapshot.hasFolder(id: Int): Boolean = folders.any { it.id == id }

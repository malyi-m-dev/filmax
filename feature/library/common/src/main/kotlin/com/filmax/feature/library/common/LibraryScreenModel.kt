package com.filmax.feature.library.common

import com.filmax.core.domain.common.firstErrorMessage
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.downloads.DownloadsRepository
import com.filmax.core.domain.favorites.FavoritesRepository
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.user.model.BookmarkFolder
import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.core.presentation.BaseScreenModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class LibraryScreenModel(
    private val watching: WatchingRepository,
    private val user: UserRepository,
    private val downloadsRepo: DownloadsRepository,
    private val favoritesRepo: FavoritesRepository,
) : BaseScreenModel<LibraryState, LibrarySideEffect, LibraryEvent>(LibraryState()) {

    init {
        onFetchData()
        observeDownloads()
        observeFavorites()
    }

    private fun observeDownloads() {
        screenModelScope {
            downloadsRepo.downloads.collect { items ->
                updateState { it.copy(downloads = items) }
            }
        }
    }

    private fun observeFavorites() {
        screenModelScope {
            favoritesRepo.favorites.collect { items ->
                updateState { it.copy(favorites = items) }
            }
        }
    }

    override fun dispatch(event: LibraryEvent) {
        when (event) {
            is LibraryEvent.TabChange -> screenModelScope { _ ->
                updateState { it.copy(tab = event.tab) }
            }
            is LibraryEvent.RemoveFromHistory -> removeFromHistory(event.itemId)
            LibraryEvent.ClearHistory -> clearHistory()
            is LibraryEvent.OpenFolder -> openFolder(event.folder)
            LibraryEvent.CloseFolder -> closeFolder()
            LibraryEvent.LoadMoreFolderItems -> loadMoreFolderItems()
            LibraryEvent.ToggleHistoryHidden -> toggleHistoryHidden()
        }
    }

    override fun onFetchData() {
        screenModelScope {
            coroutineScope {
                val historyDeferred = async { watching.getHistory() }
                val listsDeferred = async { user.getBookmarkFolders() }
                val history = historyDeferred.await()
                val lists = listsDeferred.await()
                updateState {
                    it.copy(
                        loading = false,
                        history = history.getOrNull().orEmpty(),
                        lists = lists.getOrNull().orEmpty(),
                        error = firstErrorMessage(history, lists),
                    )
                }
            }
        }
    }

    private fun removeFromHistory(itemId: Int) {
        screenModelScope {
            watching.clearHistory(itemId)
            updateState { s -> s.copy(history = s.history.filter { it.itemId != itemId }) }
        }
    }

    private fun clearHistory() {
        val ids = state.history.map { it.itemId }
        screenModelScope { _ ->
            ids.forEach { id -> watching.clearHistory(id) }
            updateState { it.copy(history = emptyList()) }
        }
    }

    /** Открывает папку-закладку и грузит первую страницу её содержимого. */
    private fun openFolder(folder: BookmarkFolder) {
        screenModelScope { _ ->
            updateState { it.copy(openFolder = OpenBookmarkFolder(folder = folder)) }
            val result = user.getBookmarkItems(folder.id)
            val itemPage = result.getOrNull()
            updateState { current ->
                val open = current.openFolder ?: return@updateState current
                // Пока грузили, папку могли закрыть или открыть другую — чужой ответ не применяем.
                if (open.folder.id != folder.id) return@updateState current
                current.copy(
                    openFolder = open.copy(
                        items = itemPage?.items.orEmpty(),
                        page = FIRST_PAGE,
                        loading = false,
                        endReached = itemPage?.pagination?.hasNextPage != true,
                        error = firstErrorMessage(result),
                    ),
                )
            }
        }
    }

    private fun closeFolder() {
        screenModelScope { _ -> updateState { it.copy(openFolder = null) } }
    }

    /** Догружает следующую страницу открытой папки. Вызывается, когда список подходит к концу. */
    private fun loadMoreFolderItems() {
        val open = state.openFolder ?: return
        if (open.loading || open.loadingMore || open.endReached) return
        val nextPage = open.page + 1
        screenModelScope { _ ->
            updateState { current -> current.copy(openFolder = current.openFolder?.copy(loadingMore = true)) }
            val result = user.getBookmarkItems(open.folder.id, nextPage)
            val itemPage = result.getOrNull()
            updateState { current ->
                val loaded = current.openFolder ?: return@updateState current
                if (loaded.folder.id != open.folder.id) return@updateState current
                current.copy(
                    openFolder = loaded.copy(
                        // Страницы kino.pub могут пересечься: дубликат id уронил бы LazyGrid по key.
                        items = (loaded.items + itemPage?.items.orEmpty()).distinctBy { it.id },
                        page = if (itemPage != null) nextPage else loaded.page,
                        loadingMore = false,
                        // Сбой страницы — не конец списка: следующая попытка повторит тот же запрос.
                        endReached = itemPage?.pagination?.hasNextPage?.not() ?: loaded.endReached,
                        error = firstErrorMessage(result),
                    ),
                )
            }
        }
    }

    private fun toggleHistoryHidden() {
        screenModelScope { _ -> updateState { it.copy(historyHidden = !it.historyHidden) } }
    }

    private companion object {
        /** Первая страница содержимого папки (нумерация kino.pub — с единицы). */
        const val FIRST_PAGE = 1
    }
}

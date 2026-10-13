package com.filmax.feature.library.common

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.common.firstErrorMessage
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.usecase.bookmarks.BookmarkFolders
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.user.model.BookmarkFolder
import com.filmax.core.domain.user.model.userFolders
import com.filmax.core.presentation.BaseScreenModel
import com.filmax.core.presentation.folders.FolderDraft
import com.filmax.feature.library.common.navigation.FolderRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

/**
 * Открытая папка-закладка на ТВ: содержимое по страницам, удаление тайтла долгим «ОК»,
 * переименование и цвет (локально — у kino.pub их нет) и удаление папки.
 *
 * Шапка — серверная папка с наложенным оформлением, потоком: переименование видно сразу.
 */
// По короткому обработчику на событие: содержимое, переименование, удаление — один экран.
@Suppress("TooManyFunctions")
class FolderScreenModel(
    savedStateHandle: SavedStateHandle,
    private val user: UserRepository,
    private val folders: BookmarkFolders,
) : BaseScreenModel<FolderState, FolderSideEffect, FolderEvent>(FolderState()) {

    private val folderId = savedStateHandle.toRoute<FolderRoute>().folderId

    /** Папка, как её знает сервер; null — ещё не загружена или её уже нет. */
    private val serverFolder = MutableStateFlow<BookmarkFolder?>(null)

    init {
        onFetchData()
        observeFolder()
    }

    override fun dispatch(event: FolderEvent) {
        when (event) {
            FolderEvent.LoadMore -> loadMore()
            is FolderEvent.RemoveItem -> removeItem(event.itemId)
            FolderEvent.StartRename -> startRename()
            is FolderEvent.EditFolder -> update { it.copy(draft = it.draft?.apply(event.input)) }
            FolderEvent.SubmitRename -> submitRename()
            FolderEvent.CancelRename -> update { it.copy(draft = null) }
            FolderEvent.AskDelete -> update { it.copy(confirmDelete = true) }
            FolderEvent.DismissDelete -> update { it.copy(confirmDelete = false) }
            FolderEvent.ConfirmDelete -> deleteFolder()
        }
    }

    override fun onFetchData() {
        screenModelScope { _ ->
            val listed = folders.list()
            val found = listed.getOrNull()?.firstOrNull { it.id == folderId }
            serverFolder.value = found
            val firstPage = user.getBookmarkItems(folderId)
            val itemPage = firstPage.getOrNull()
            // Шапка — вместе с первой страницей: экран не мелькает «папка не найдена», пока
            // поток оформления догоняет загрузку.
            val styled = found?.let { userFolders(listOf(it), folders.appearances.first()).firstOrNull() }
            updateState {
                it.copy(
                    loading = false,
                    folder = styled,
                    items = itemPage?.items.orEmpty().distinctBy { item -> item.id },
                    page = if (itemPage != null) FIRST_PAGE else 0,
                    endReached = itemPage?.pagination?.hasNextPage != true,
                    error = firstErrorMessage(listed, firstPage),
                )
            }
        }
    }

    private fun observeFolder() {
        screenModelScope { _ ->
            combine(serverFolder.filterNotNull(), folders.appearances) { folder, looks ->
                userFolders(listOf(folder), looks).firstOrNull()
            }.collect { folder -> updateState { it.copy(folder = folder) } }
        }
    }

    /** Следующая страница содержимого. Страницы kino.pub могут пересечься — дубли отбрасываются. */
    private fun loadMore() {
        screenModelScope { current ->
            if (current.loading || current.loadingMore || current.endReached) return@screenModelScope
            val nextPage = current.page + 1
            updateState { it.copy(loadingMore = true) }
            val result = user.getBookmarkItems(folderId, nextPage)
            val itemPage = result.getOrNull()
            updateState {
                it.copy(
                    items = (it.items + itemPage?.items.orEmpty()).distinctBy { item -> item.id },
                    page = if (itemPage != null) nextPage else it.page,
                    loadingMore = false,
                    // Сбой страницы — не конец списка: следующая попытка повторит тот же запрос.
                    endReached = itemPage?.pagination?.hasNextPage?.not() ?: it.endReached,
                    error = firstErrorMessage(result),
                )
            }
        }
    }

    /** Убирает тайтл сразу (отклик мгновенный); не вышло на сервере — возвращает на место. */
    private fun removeItem(itemId: Int) {
        screenModelScope { current ->
            val before = current.items
            updateState { it.copy(items = it.items.filterNot { item -> item.id == itemId }) }
            serverFolder.value = serverFolder.value?.let { folder -> folder.copy(count = folder.count - 1) }
            val result = user.removeFromBookmark(itemId, folderId)
            if (result is RequestResult.Error) {
                updateState { it.copy(items = before, error = result.message) }
                serverFolder.value = serverFolder.value?.let { folder -> folder.copy(count = folder.count + 1) }
            }
        }
    }

    private fun startRename() = update { current ->
        val folder = current.folder ?: return@update current
        current.copy(draft = FolderDraft(title = folder.title, tint = folder.tint, folderId = folder.id))
    }

    /**
     * Черновик читается в момент выполнения, а не отправки события: правки с клавиатуры стоят в
     * той же очереди раньше, и «Сохранить» должен увидеть их все.
     */
    private fun submitRename() {
        screenModelScope { current ->
            val draft = current.draft?.takeIf { it.canSubmit } ?: return@screenModelScope
            updateState { it.copy(draft = null) }
            folders.restyle(folderId, draft.cleanTitle, draft.tint)
        }
    }

    private fun deleteFolder() {
        screenModelScope { _ ->
            updateState { it.copy(confirmDelete = false) }
            when (val result = folders.delete(folderId)) {
                is RequestResult.Success -> postSideEffect(FolderSideEffect.Deleted)
                is RequestResult.Error -> updateState { it.copy(error = result.message) }
            }
        }
    }

    private fun update(transform: (FolderState) -> FolderState) {
        screenModelScope { _ -> updateState(transform) }
    }

    private companion object {
        /** Первая страница содержимого папки (нумерация kino.pub — с единицы). */
        const val FIRST_PAGE = 1
    }
}

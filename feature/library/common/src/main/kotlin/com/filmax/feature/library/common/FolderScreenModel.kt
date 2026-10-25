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
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.presentation.BaseScreenModel
import com.filmax.core.presentation.folders.FolderDraft
import com.filmax.core.presentation.folders.FolderInput
import com.filmax.feature.library.common.navigation.FolderRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

/**
 * Открытая папка-закладка (телефон и ТВ): содержимое по страницам, удаление тайтла долгим «ОК»,
 * переименование и цвет (локально — у kino.pub их нет), удаление папки и переходы в карточку и плеер.
 *
 * Шапка — серверная папка с наложенным оформлением, потоком: переименование видно сразу.
 * Обработчики читают состояние внутри `screenModelScope { current -> }`: события стоят в одной
 * очереди, и правка черновика должна видеть результат предыдущей.
 */
class FolderScreenModel(
    savedStateHandle: SavedStateHandle,
    private val user: UserRepository,
    private val folders: BookmarkFolders,
    private val navigator: Navigator,
) : BaseScreenModel<FolderState, FolderSideEffect, FolderEvent>(FolderState()) {

    private val folderId = savedStateHandle.toRoute<FolderRoute>().folderId

    /** Папка, как её знает сервер; null — ещё не загружена или её уже нет. */
    private val serverFolder = MutableStateFlow<BookmarkFolder?>(null)

    init {
        onFetchData()
        observeFolder()
    }

    override fun onEvent(event: FolderEvent) {
        when (event) {
            FolderEvent.LoadMore -> onLoadMore()
            is FolderEvent.RemoveItem -> onRemoveItem(event.itemId)
            FolderEvent.StartRename -> onStartRename()
            is FolderEvent.EditFolder -> onEditFolder(event.input)
            FolderEvent.SubmitRename -> onSubmitRename()
            FolderEvent.CancelRename -> onCancelRename()
            FolderEvent.AskDelete -> onDeleteDialog(open = true)
            FolderEvent.DismissDelete -> onDeleteDialog(open = false)
            FolderEvent.ConfirmDelete -> onConfirmDelete()
            FolderEvent.Back -> navigator.back()
            is FolderEvent.OpenItem -> navigator.open(Destination.Details(event.itemId))
            FolderEvent.PlayAll -> onPlayAll()
            FolderEvent.OpenCatalog -> navigator.open(Destination.Catalog)
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
            val items = itemPage?.items.orEmpty().distinctBy { item -> item.id }
            val page = if (itemPage != null) FIRST_PAGE else 0
            val endReached = itemPage?.pagination?.hasNextPage != true
            val error = firstErrorMessage(listed, firstPage)
            updateState {
                it.copy(
                    loading = false,
                    folder = styled,
                    items = items,
                    page = page,
                    endReached = endReached,
                    error = error,
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
    private fun onLoadMore() {
        screenModelScope { current ->
            if (current.loading || current.loadingMore || current.endReached) return@screenModelScope
            val nextPage = current.page + 1
            updateState { it.copy(loadingMore = true) }
            val result = user.getBookmarkItems(folderId, nextPage)
            val itemPage = result.getOrNull()
            val items = (state.items + itemPage?.items.orEmpty()).distinctBy { item -> item.id }
            val page = if (itemPage != null) nextPage else state.page
            // Сбой страницы — не конец списка: следующая попытка повторит тот же запрос.
            val endReached = itemPage?.pagination?.hasNextPage?.not() ?: state.endReached
            val error = firstErrorMessage(result)
            updateState {
                it.copy(items = items, page = page, loadingMore = false, endReached = endReached, error = error)
            }
        }
    }

    /** Убирает тайтл сразу (отклик мгновенный); не вышло на сервере — возвращает на место. */
    private fun onRemoveItem(itemId: Int) {
        screenModelScope { current ->
            val before = current.items
            val without = before.filterNot { item -> item.id == itemId }
            updateState { it.copy(items = without) }
            serverFolder.value = serverFolder.value?.let { folder -> folder.copy(count = folder.count - 1) }
            val result = user.removeFromBookmark(itemId, folderId)
            if (result is RequestResult.Error) {
                updateState { it.copy(items = before, error = result.message) }
                serverFolder.value = serverFolder.value?.let { folder -> folder.copy(count = folder.count + 1) }
            }
        }
    }

    private fun onStartRename() {
        screenModelScope { current ->
            val folder = current.folder ?: return@screenModelScope
            val draft = FolderDraft(title = folder.title, tint = folder.tint, folderId = folder.id)
            updateState { it.copy(draft = draft) }
        }
    }

    private fun onEditFolder(input: FolderInput) {
        screenModelScope { current ->
            val draft = current.draft?.apply(input)
            updateState { it.copy(draft = draft) }
        }
    }

    private fun onCancelRename() {
        screenModelScope { _ -> updateState { it.copy(draft = null) } }
    }

    /** «Сохранить» видит все правки с клавиатуры: они стоят в той же очереди раньше него. */
    private fun onSubmitRename() {
        screenModelScope { current ->
            val draft = current.draft?.takeIf { it.canSubmit } ?: return@screenModelScope
            updateState { it.copy(draft = null) }
            folders.restyle(folderId, draft.cleanTitle, draft.tint)
        }
    }

    private fun onDeleteDialog(open: Boolean) {
        screenModelScope { _ -> updateState { it.copy(confirmDelete = open) } }
    }

    /** Папку удалили — экрану больше нечего показывать, назад в «Моё». */
    private fun onConfirmDelete() {
        screenModelScope { _ ->
            updateState { it.copy(confirmDelete = false) }
            when (val result = folders.delete(folderId)) {
                is RequestResult.Success -> navigator.back()
                is RequestResult.Error -> updateState { it.copy(error = result.message) }
            }
        }
    }

    private fun onPlayAll() {
        val first = state.items.firstOrNull() ?: return
        navigator.open(Destination.Player(itemId = first.id))
    }

    private companion object {
        /** Первая страница содержимого папки (нумерация kino.pub — с единицы). */
        const val FIRST_PAGE = 1
    }
}

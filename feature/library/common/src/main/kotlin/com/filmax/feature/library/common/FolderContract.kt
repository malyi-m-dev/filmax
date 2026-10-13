package com.filmax.feature.library.common

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.user.model.UserFolder
import com.filmax.core.presentation.folders.FolderDraft
import com.filmax.core.presentation.folders.FolderInput

/**
 * Экран открытой папки (ТВ). Содержимое постраничное — kino.pub отдаёт `bookmarks/{id}` по
 * страницам; [folder] — папка с локальным оформлением (цвет шапки, название).
 */
data class FolderState(
    val loading: Boolean = true,
    val folder: UserFolder? = null,
    val items: List<Item> = emptyList(),
    /** Последняя загруженная страница (0 — ещё ни одной). */
    val page: Int = 0,
    val loadingMore: Boolean = false,
    val endReached: Boolean = false,
    /** Черновик переименования; не null — открыта панель «Переименовать». */
    val draft: FolderDraft? = null,
    /** Открыт диалог «Удалить папку?». */
    val confirmDelete: Boolean = false,
    val error: String? = null,
)

sealed interface FolderEvent {
    /** Сетка подошла к концу — догрузить следующую страницу. */
    data object LoadMore : FolderEvent

    /** Долгое «ОК» на постере: убрать тайтл из папки. */
    data class RemoveItem(val itemId: Int) : FolderEvent

    data object StartRename : FolderEvent
    data class EditFolder(val input: FolderInput) : FolderEvent
    data object SubmitRename : FolderEvent
    data object CancelRename : FolderEvent

    data object AskDelete : FolderEvent
    data object ConfirmDelete : FolderEvent
    data object DismissDelete : FolderEvent
}

sealed interface FolderSideEffect {
    /** Папку удалили — экрану больше нечего показывать, пора назад в «Моё». */
    data object Deleted : FolderSideEffect
}

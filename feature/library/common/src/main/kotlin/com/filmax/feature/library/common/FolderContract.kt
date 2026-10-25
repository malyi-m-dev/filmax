package com.filmax.feature.library.common

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.user.model.UserFolder
import com.filmax.core.presentation.folders.FolderDraft
import com.filmax.core.presentation.folders.FolderInput

/**
 * Экран открытой папки (телефон и ТВ). Содержимое постраничное — kino.pub отдаёт `bookmarks/{id}` по
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

    data object Back : FolderEvent
    data class OpenItem(val itemId: Int) : FolderEvent

    /** «Смотреть подряд»: первый тайтл папки целиком — фильм или первая серия сериала. */
    data object PlayAll : FolderEvent

    /** Пустая папка: «Перейти в каталог» — единственное осмысленное действие. */
    data object OpenCatalog : FolderEvent
}

/** Одноразовых эффектов у папки нет: удалили — модель сама уводит назад в «Моё». */
sealed interface FolderSideEffect

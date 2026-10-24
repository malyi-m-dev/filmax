package com.filmax.feature.details.common

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.favorites.FavoritesRepository
import com.filmax.core.domain.usecase.bookmarks.BookmarkFolders
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.user.model.BookmarkFolder
import com.filmax.core.domain.user.model.UserFolder
import com.filmax.core.domain.user.model.userFolders
import com.filmax.core.presentation.BaseScreenModel
import com.filmax.core.presentation.folders.FolderDraft
import com.filmax.feature.details.common.navigation.DetailsRoute
import kotlinx.coroutines.flow.first

/**
 * Панель «В папку» тайтла из маршрута деталей: какие папки его содержат, отметка «ОК» и создание
 * папки сразу с тайтлом. Отдельная модель, а не часть [DetailsScreenModel]: панель — свой
 * сценарий со своими данными (папки, их содержимое); на ТВ это боковая панель, на телефоне — шторка.
 *
 * Отметки оптимистичные: галочка встаёт сразу, запрос догоняет; ответ с ошибкой откатывает её и
 * оставляет в панели сообщение.
 */
// По короткому обработчику на событие панели: открыть, отметить, создать, отменить.
@Suppress("TooManyFunctions")
class FolderPickerScreenModel(
    savedStateHandle: SavedStateHandle,
    private val user: UserRepository,
    private val folders: BookmarkFolders,
    private val favorites: FavoritesRepository,
) : BaseScreenModel<FolderPickerState, FolderPickerSideEffect, FolderPickerEvent>(FolderPickerState()) {

    private val itemId = savedStateHandle.toRoute<DetailsRoute>().itemId

    init {
        observeWatchlistCount()
    }

    override fun onEvent(event: FolderPickerEvent) {
        when (event) {
            FolderPickerEvent.Open -> open()
            FolderPickerEvent.Close -> update { it.copy(open = false, draft = null, error = null) }
            is FolderPickerEvent.Toggle -> toggle(event.folderId)
            FolderPickerEvent.StartCreate -> update { it.copy(draft = FolderDraft(), error = null) }
            is FolderPickerEvent.Edit -> update { it.copy(draft = it.draft?.apply(event.input)) }
            FolderPickerEvent.Submit -> submit()
            FolderPickerEvent.CancelCreate -> update { it.copy(draft = null, error = null) }
        }
    }

    /** Папки грузятся при открытии панели, а не с деталями: проверка стоит запроса на папку. */
    override fun onFetchData() {
        screenModelScope { _ ->
            val listed = folders.list().getOrNull().orEmpty()
            val mine = sortedByChange(userFolders(listed, folders.appearances.first()))
            // Только папки пользователя: «Буду смотреть» отмечается своей строкой, и из-за неё
            // одной иконка «В папку» не должна заливаться.
            val mineIds = mine.map { it.id }.toSet()
            val memberOf = folders.itemFolders(itemId, listed.filter { it.id in mineIds })
            updateState {
                it.copy(
                    loading = false,
                    folders = mine,
                    memberOf = memberOf,
                    focusFolderId = mine.firstOrNull { folder -> folder.id !in memberOf }?.id,
                )
            }
        }
    }

    private fun open() {
        update { it.copy(open = true, loading = true, draft = null, error = null) }
        onFetchData()
    }

    private fun observeWatchlistCount() {
        screenModelScope { _ ->
            favorites.favorites.collect { items -> updateState { it.copy(watchlistCount = items.size) } }
        }
    }

    /** Отметка сразу, запрос следом; сбой — откат отметки и счётчика и сообщение в панели. */
    private fun toggle(folderId: Int) {
        screenModelScope { current ->
            val wasIn = folderId in current.memberOf
            updateState { it.withMembership(folderId, member = !wasIn).copy(error = null) }
            val result = if (wasIn) user.removeFromBookmark(itemId, folderId) else user.addToBookmark(itemId, folderId)
            if (result is RequestResult.Error) {
                updateState { it.withMembership(folderId, member = wasIn).copy(error = SYNC_ERROR) }
            }
        }
    }

    /**
     * «Создать и добавить»: папка, затем тайтл в неё, и возврат к списку — новая папка наверху и в
     * фокусе. Не создалась — редактор остаётся с набранным названием и сообщением. Создалась, но
     * тайтл не лёг — папка в списке без отметки: показать её отмеченной значило бы соврать.
     */
    private fun submit() {
        screenModelScope { current ->
            // Черновик — на момент выполнения: правки с клавиатуры стоят в очереди раньше.
            val draft = current.draft?.takeIf { it.canSubmit && !current.creating } ?: return@screenModelScope
            updateState { it.copy(creating = true, error = null) }
            val created = folders.create(draft.cleanTitle, draft.tint).getOrNull()
            if (created == null) {
                updateState { it.copy(creating = false, error = CREATE_ERROR) }
                return@screenModelScope
            }
            val added = user.addToBookmark(itemId, created.id) is RequestResult.Success
            // Перечитать список не вышло — новая папка всё равно встаёт наверх, с выбранным цветом.
            val mine = reloadedFolders() ?: (listOf(created.asNewFolder(draft.tint, added)) + current.folders)
            updateState {
                it.copy(
                    creating = false,
                    draft = null,
                    folders = mine,
                    memberOf = if (added) it.memberOf + created.id else it.memberOf,
                    focusFolderId = created.id,
                    error = if (added) null else ADD_ERROR,
                )
            }
        }
    }

    private suspend fun reloadedFolders(): List<UserFolder>? =
        folders.list().getOrNull()?.let { listed -> sortedByChange(userFolders(listed, folders.appearances.first())) }

    private fun update(transform: (FolderPickerState) -> FolderPickerState) {
        screenModelScope { _ -> updateState(transform) }
    }

    private companion object {
        const val SYNC_ERROR = "Не удалось обновить папку — проверьте сеть"
        const val CREATE_ERROR = "Не удалось создать папку — проверьте сеть"
        const val ADD_ERROR = "Папка создана, но тайтл в неё не добавился — проверьте сеть"
    }
}

/** Папки по дате изменения, свежие первыми; новая (без даты с сервера) — первой. */
private fun sortedByChange(folders: List<UserFolder>): List<UserFolder> =
    folders.sortedByDescending { it.updatedAt ?: Long.MAX_VALUE }

/** Только что созданная папка: сервер о ней ещё не рассказал, но id и название известны. */
private fun BookmarkFolder.asNewFolder(tint: Int, withItem: Boolean) = UserFolder(
    id = id,
    title = title,
    count = if (withItem) 1 else 0,
    updatedAt = null,
    tint = tint
)

/** Отметка тайтла в папке вместе со счётчиком папки — он виден в подписи строки. */
private fun FolderPickerState.withMembership(folderId: Int, member: Boolean): FolderPickerState {
    if ((folderId in memberOf) == member) return this
    val delta = if (member) 1 else -1
    return copy(
        memberOf = if (member) memberOf + folderId else memberOf - folderId,
        folders = folders.map { folder ->
            if (folder.id == folderId) {
                folder.copy(
                    count = folder.count + delta
                )
            } else {
                folder
            }
        },
    )
}

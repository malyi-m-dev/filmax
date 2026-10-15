package com.filmax.feature.library.common

import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.common.firstErrorMessage
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.favorites.FavoritesRepository
import com.filmax.core.domain.usecase.bookmarks.BookmarkFolders
import com.filmax.core.domain.usecase.bookmarks.FolderShelf
import com.filmax.core.domain.usecase.watching.RemoveFromWatchLaterUseCase
import com.filmax.core.domain.user.model.FolderAppearance
import com.filmax.core.domain.user.model.userFolders
import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.presentation.BaseScreenModel
import com.filmax.core.presentation.folders.FolderDraft
import com.filmax.core.presentation.folders.FolderInput
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.ZonedDateTime

/**
 * ТВ-раздел «Моё»: сегменты «Продолжить · Буду смотреть · Папки · История», сортировки, удаление
 * долгим «ОК», создание папки и очистка истории. Телефонная Библиотека живёт на своей модели
 * ([LibraryScreenModel]) — у неё другие вкладки; общие правила вынесены в домен и MineFormat.
 *
 * Состояние собирается реактивно: избранное и оформление папок — потоки, поэтому смена порядка,
 * перекраска и удаление видны сразу, без перезагрузки. [clock] нужен истории («Сегодня»,
 * «Вчера») и подписям папок — тесты подставляют фиксированное «сейчас».
 */
// Модель держит по короткому обработчику на событие (сегменты, сортировки, удаление, черновик
// папки, история): дробить их по классам значило бы разнести один экран по нескольким моделям.
@Suppress("TooManyFunctions")
class MineScreenModel(
    private val watching: WatchingRepository,
    private val favorites: FavoritesRepository,
    private val folders: BookmarkFolders,
    private val removeFromWatchLater: RemoveFromWatchLaterUseCase,
    private val clock: Clock,
) : BaseScreenModel<MineState, MineSideEffect, MineEvent>(MineState()) {

    /** Последний ответ сервера по папкам — сырьё, поверх которого ложатся оформление и порядок. */
    private val shelf = MutableStateFlow<FolderShelf?>(null)

    init {
        onFetchData()
        observeWatchlist()
        observeFolders()
    }

    override fun onEvent(event: MineEvent) {
        when (event) {
            is MineEvent.SelectSegment -> update { it.copy(segment = event.segment) }
            MineEvent.CycleWatchlistSort -> update { it.copy(watchlistSort = it.watchlistSort.next()) }
            MineEvent.CycleFolderSort -> update { it.copy(folderSort = it.folderSort.next()) }
            is MineEvent.RemoveFromWatchlist -> screenModelScope { _ -> removeFromWatchLater(event.itemId) }
            is MineEvent.RemoveFromContinue -> removeFromHistory(event.itemId)
            MineEvent.AskClearHistory -> update { it.copy(confirmClearHistory = true) }
            MineEvent.DismissClearHistory -> update { it.copy(confirmClearHistory = false) }
            MineEvent.ConfirmClearHistory -> clearHistory()
            MineEvent.StartCreateFolder -> update { it.copy(draft = FolderDraft()) }
            is MineEvent.EditFolder -> editDraft(event.input)
            MineEvent.SubmitFolder -> submitFolder()
            MineEvent.CancelFolder -> update { it.copy(draft = null) }
            MineEvent.Refresh -> refresh()
        }
    }

    override fun onFetchData() {
        screenModelScope { _ ->
            coroutineScope {
                val historyRequest = async { watching.getHistory() }
                val shelfRequest = async { folders.shelf() }
                val history = historyRequest.await()
                val loaded = shelfRequest.await()
                applyHistory(history)
                loaded.getOrNull()?.let { shelf.value = it }
                updateState { it.copy(loading = false, error = firstErrorMessage(history, loaded)) }
            }
        }
    }

    /** Повторный показ экрана: пока идёт первая загрузка, второй запрос ни к чему. */
    private fun refresh() {
        screenModelScope { current -> if (!current.loading) onFetchData() }
    }

    private fun observeWatchlist() {
        screenModelScope { _ ->
            val sort = states.map { it.watchlistSort }.distinctUntilChanged()
            combine(favorites.favorites, sort) { items, order -> sortedWatchlist(items, order) }
                .collect { items -> updateState { it.copy(watchlist = items) } }
        }
    }

    private fun observeFolders() {
        screenModelScope { _ ->
            val sort = states.map { it.folderSort }.distinctUntilChanged()
            combine(shelf.filterNotNull(), folders.appearances, sort) { loaded, looks, order ->
                sortedFolders(mineFolders(loaded, looks), order)
            }.collect { items -> updateState { it.copy(folders = items) } }
        }
    }

    private fun mineFolders(loaded: FolderShelf, looks: Map<Int, FolderAppearance>): List<MineFolder> {
        val now = now()
        return userFolders(loaded.folders, looks).map { folder ->
            MineFolder(folder, loaded.posters[folder.id].orEmpty(), changedLabel(folder.updatedAt, now))
        }
    }

    private suspend fun applyHistory(result: RequestResult<List<WatchHistory>>) {
        val entries = result.getOrNull() ?: return
        updateState { it.copy(continueWatching = continueEntries(entries), history = historyDays(entries, now())) }
    }

    /** Убирает тайтл из «Продолжить» сразу, затем просит сервер забыть его историю. */
    private fun removeFromHistory(itemId: Int) {
        screenModelScope { _ ->
            updateState { current ->
                current.copy(continueWatching = current.continueWatching.filterNot { it.itemId == itemId })
            }
            watching.clearHistory(itemId)
            applyHistory(watching.getHistory())
        }
    }

    /** kino.pub чистит историю по тайтлу — поэтому по запросу на каждый тайтл истории. */
    private fun clearHistory() {
        screenModelScope { current ->
            val ids = current.history.flatMap { day -> day.entries.map { it.entry.itemId } }
            updateState { it.copy(confirmClearHistory = false, history = emptyList(), continueWatching = emptyList()) }
            ids.forEach { id -> watching.clearHistory(id) }
        }
    }

    private fun editDraft(input: FolderInput) = update { current -> current.copy(draft = current.draft?.apply(input)) }

    /**
     * Создаёт папку и перечитывает список: id и порядок задаёт сервер, а угаданный локально id
     * сломал бы открытие новой папки. Панель закрывается сразу — плитка появится с ответом.
     * Черновик читается при выполнении: правки с клавиатуры стоят в очереди раньше «Создать».
     */
    private fun submitFolder() {
        screenModelScope { current ->
            val draft = current.draft?.takeIf { it.canSubmit } ?: return@screenModelScope
            updateState { it.copy(draft = null) }
            folders.create(draft.cleanTitle, draft.tint)
            folders.shelf().getOrNull()?.let { shelf.value = it }
        }
    }

    private fun update(transform: (MineState) -> MineState) {
        screenModelScope { _ -> updateState(transform) }
    }

    private fun now(): ZonedDateTime = ZonedDateTime.now(clock)
}

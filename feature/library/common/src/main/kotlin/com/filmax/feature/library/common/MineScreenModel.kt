package com.filmax.feature.library.common

import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.common.firstErrorMessage
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.source.ContentSourceRepository
import com.filmax.core.domain.source.capabilities
import com.filmax.core.domain.usecase.bookmarks.BookmarkFolders
import com.filmax.core.domain.usecase.bookmarks.FolderShelf
import com.filmax.core.domain.user.model.FolderAppearance
import com.filmax.core.domain.user.model.userFolders
import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.domain.watchlist.WatchlistRepository
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
 * Раздел «Моё» телефона и ТВ: сегменты «Продолжить · Буду смотреть · Папки · История», сортировки,
 * удаление (долгое «ОК» на пульте, долгий тап на телефоне), создание папки и очистка истории.
 * Общие правила вынесены в домен и MineFormat.
 *
 * Состояние собирается реактивно: «Буду смотреть» и оформление папок — потоки, поэтому смена порядка,
 * перекраска и удаление видны сразу, без перезагрузки. [clock] нужен истории («Сегодня»,
 * «Вчера») и подписям папок — тесты подставляют фиксированное «сейчас».
 */
// Модель держит по короткому обработчику на событие (сегменты, сортировки, удаление, черновик
// папки, история): дробить их по классам значило бы разнести один экран по нескольким моделям.
@Suppress("TooManyFunctions")
class MineScreenModel(
    private val watching: WatchingRepository,
    private val watchlist: WatchlistRepository,
    private val folders: BookmarkFolders,
    private val clock: Clock,
    sources: ContentSourceRepository,
) : BaseScreenModel<MineState, MineSideEffect, MineEvent>(MineState(segments = segmentsOf(sources))) {

    /** Папок у источника нет — их и не запрашиваем: ответ всё равно пустой. */
    private val foldersAvailable = MineSegment.FOLDERS in state.segments

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
            is MineEvent.RemoveFromWatchlist -> screenModelScope { watchlist.remove(event.itemId) }
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
        screenModelScope {
            coroutineScope {
                val historyRequest = async { watching.getHistory() }
                val shelfRequest = async { if (foldersAvailable) folders.shelf() else RequestResult.Success(null) }
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
        screenModelScope { if (!state.loading) onFetchData() }
    }

    private fun observeWatchlist() {
        screenModelScope {
            val sort = states.map { it.watchlistSort }.distinctUntilChanged()
            combine(watchlist.items, sort) { items, order -> sortedWatchlist(items, order) }
                .collect { items -> updateState { it.copy(watchlist = items) } }
        }
    }

    private fun observeFolders() {
        screenModelScope {
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
        screenModelScope {
            updateState { current ->
                current.copy(continueWatching = current.continueWatching.filterNot { it.itemId == itemId })
            }
            watching.clearHistory(itemId)
            applyHistory(watching.getHistory())
        }
    }

    /** kino.pub чистит историю по тайтлу — поэтому по запросу на каждый тайтл истории. */
    private fun clearHistory() {
        screenModelScope {
            val current = state
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
        screenModelScope {
            val current = state
            val draft = current.draft?.takeIf { it.canSubmit } ?: return@screenModelScope
            updateState { it.copy(draft = null) }
            folders.create(draft.cleanTitle, draft.tint)
            folders.shelf().getOrNull()?.let { shelf.value = it }
        }
    }

    private fun update(transform: (MineState) -> MineState) {
        screenModelScope { updateState(transform) }
    }

    private fun now(): ZonedDateTime = ZonedDateTime.now(clock)
}

/** Сегменты «Моего» для источника: «Папки» — только где они есть. */
private fun segmentsOf(sources: ContentSourceRepository): List<MineSegment> {
    val capabilities = sources.source.value.capabilities
    return MineSegment.entries.filter { it != MineSegment.FOLDERS || capabilities.folders }
}

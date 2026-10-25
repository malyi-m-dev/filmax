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
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.presentation.BaseScreenModel
import com.filmax.core.presentation.folders.FolderDraft
import com.filmax.core.presentation.folders.FolderInput
import com.filmax.core.presentation.labels.titlesLabel
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
 * удаление (долгое «ОК» на пульте, долгий тап с подтверждением на телефоне), создание папки,
 * очистка истории и переходы в карточку, плеер, папку и поиск. Общие правила — в MineFormat.
 *
 * Состояние собирается реактивно: избранное и оформление папок — потоки, поэтому смена порядка,
 * перекраска и удаление видны сразу, без перезагрузки. [clock] нужен истории («Сегодня»,
 * «Вчера») и подписям папок — тесты подставляют фиксированное «сейчас».
 *
 * Обработчики читают состояние внутри `screenModelScope { current -> }`, в момент выполнения:
 * события стоят в одной очереди, и правка черновика должна видеть результат предыдущей.
 */
class MineScreenModel(
    private val watching: WatchingRepository,
    private val favorites: FavoritesRepository,
    private val folders: BookmarkFolders,
    private val removeFromWatchLater: RemoveFromWatchLaterUseCase,
    private val clock: Clock,
    private val navigator: Navigator,
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
            is MineEvent.SelectSegment -> onSelectSegment(event.segment)
            MineEvent.CycleWatchlistSort -> onCycleWatchlistSort()
            MineEvent.CycleFolderSort -> onCycleFolderSort()
            is MineEvent.RemoveFromWatchlist -> onRemoveFromWatchlist(event.itemId)
            is MineEvent.RemoveFromContinue -> onRemoveFromContinue(event.itemId)
            is MineEvent.AskRemoval -> onRemovalSheet(event.removal)
            MineEvent.DismissRemoval -> onRemovalSheet(removal = null)
            MineEvent.ConfirmRemoval -> onConfirmRemoval()
            MineEvent.AskClearHistory -> onClearHistoryDialog(open = true)
            MineEvent.DismissClearHistory -> onClearHistoryDialog(open = false)
            MineEvent.ConfirmClearHistory -> onConfirmClearHistory()
            MineEvent.StartCreateFolder -> onStartCreateFolder()
            is MineEvent.EditFolder -> onEditFolder(event.input)
            MineEvent.SubmitFolder -> onSubmitFolder()
            MineEvent.CancelFolder -> onCancelFolder()
            is MineEvent.OpenItem -> navigator.open(Destination.Details(event.itemId))
            is MineEvent.ContinueWatching -> onContinueWatching(event.entry)
            is MineEvent.OpenFolder -> navigator.open(Destination.BookmarkFolder(event.folderId))
            MineEvent.OpenSearch -> navigator.open(Destination.Search)
            MineEvent.Refresh -> onRefresh()
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
                val error = firstErrorMessage(history, loaded)
                updateState { it.copy(loading = false, error = error) }
            }
        }
    }

    /** Повторный показ экрана: пока идёт первая загрузка, второй запрос ни к чему. */
    private fun onRefresh() {
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
            val changed = changedLabel(folder.updatedAt, now)
            MineFolder(
                folder = folder,
                posters = loaded.posters[folder.id].orEmpty(),
                changed = changed,
                caption = listOfNotNull(titlesLabel(folder.count), changed).joinToString(" · "),
            )
        }
    }

    private suspend fun applyHistory(result: RequestResult<List<WatchHistory>>) {
        val entries = result.getOrNull() ?: return
        val continueWatching = continueEntries(entries)
        val history = historyDays(entries, now())
        updateState { it.copy(continueWatching = continueWatching, history = history) }
    }

    private fun onSelectSegment(segment: MineSegment) {
        screenModelScope { _ -> updateState { it.copy(segment = segment) } }
    }

    private fun onCycleWatchlistSort() {
        screenModelScope { current ->
            val sort = current.watchlistSort.next()
            updateState { it.copy(watchlistSort = sort) }
        }
    }

    private fun onCycleFolderSort() {
        screenModelScope { current ->
            val sort = current.folderSort.next()
            updateState { it.copy(folderSort = sort) }
        }
    }

    private fun onRemoveFromWatchlist(itemId: Int) {
        screenModelScope { _ -> removeFromWatchLater(itemId) }
    }

    /** Убирает тайтл из «Продолжить» сразу, затем просит сервер забыть его историю. */
    private fun onRemoveFromContinue(itemId: Int) {
        screenModelScope { current ->
            val continueWatching = current.continueWatching.filterNot { it.itemId == itemId }
            updateState { it.copy(continueWatching = continueWatching) }
            watching.clearHistory(itemId)
            applyHistory(watching.getHistory())
        }
    }

    private fun onRemovalSheet(removal: MineRemoval?) {
        screenModelScope { _ -> updateState { it.copy(confirmRemoval = removal) } }
    }

    /** «Убрать» в шторке телефона — то же, что долгое «ОК» на ТВ, только с подтверждением. */
    private fun onConfirmRemoval() {
        screenModelScope { current ->
            val removal = current.confirmRemoval ?: return@screenModelScope
            updateState { it.copy(confirmRemoval = null) }
            when (removal) {
                is MineRemoval.Watchlist -> onRemoveFromWatchlist(removal.itemId)
                is MineRemoval.History -> onRemoveFromContinue(removal.itemId)
            }
        }
    }

    private fun onClearHistoryDialog(open: Boolean) {
        screenModelScope { _ -> updateState { it.copy(confirmClearHistory = open) } }
    }

    /** kino.pub чистит историю по тайтлу — поэтому по запросу на каждый тайтл истории. */
    private fun onConfirmClearHistory() {
        screenModelScope { current ->
            val ids = current.history.flatMap { day -> day.entries.map { it.entry.itemId } }
            updateState { it.copy(confirmClearHistory = false, history = emptyList(), continueWatching = emptyList()) }
            ids.forEach { id -> watching.clearHistory(id) }
        }
    }

    private fun onStartCreateFolder() {
        screenModelScope { _ -> updateState { it.copy(draft = FolderDraft()) } }
    }

    private fun onEditFolder(input: FolderInput) {
        screenModelScope { current ->
            val draft = current.draft?.apply(input)
            updateState { it.copy(draft = draft) }
        }
    }

    private fun onCancelFolder() {
        screenModelScope { _ -> updateState { it.copy(draft = null) } }
    }

    /**
     * Создаёт папку и перечитывает список: id и порядок задаёт сервер, а угаданный локально id
     * сломал бы открытие новой папки. Панель закрывается сразу — плитка появится с ответом.
     */
    private fun onSubmitFolder() {
        screenModelScope { current ->
            val draft = current.draft?.takeIf { it.canSubmit } ?: return@screenModelScope
            updateState { it.copy(draft = null) }
            folders.create(draft.cleanTitle, draft.tint)
            folders.shelf().getOrNull()?.let { shelf.value = it }
        }
    }

    /** «Продолжить» и история ведут сразу в плеер: недосмотренная серия — сезон и номер из истории. */
    private fun onContinueWatching(entry: WatchHistory) {
        val progress = entry.progress
        navigator.open(
            Destination.Player(
                itemId = entry.itemId,
                videoId = progress?.videoId ?: Destination.Player.UNKNOWN,
                season = progress?.season ?: Destination.Player.UNKNOWN,
            ),
        )
    }

    private fun now(): ZonedDateTime = ZonedDateTime.now(clock)
}

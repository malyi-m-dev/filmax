// Раздел «Моё»: состояние экрана и сетка — чем наполняется каждый сегмент, сколько в нём
// колонок и когда догружать папку. Шапка с сегментами — в LibraryHeader, карточки, плитки и
// пустые состояния — в LibraryCards, диалоги закладок — в LibraryDialogs.

package com.filmax.feature.library.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.user.model.BookmarkFolder
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.ui.components.FilmaxPosterCard
import com.filmax.core.ui.components.posterMeta
import com.filmax.feature.library.common.LibraryEvent
import com.filmax.feature.library.common.LibraryScreenModel
import com.filmax.feature.library.common.LibraryState
import com.filmax.feature.library.common.OpenBookmarkFolder
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Сегменты раздела «Моё» — четыре непересекающихся ответа на вопрос «что у меня есть».
 *
 * Заменяют старые вкладки Библиотеки, которые пересекались: «Избранное» и есть «Буду смотреть»,
 * а «Загрузки» ничего не качали и убраны. Совпадают с сегментами TV-раздела, но живут отдельно:
 * общий `LibraryTab` описывал именно старые вкладки и мобильному экрану больше не нужен.
 */
internal enum class MineSegment(val label: String) {
    CONTINUE("Продолжить"),
    WATCHLIST("Буду смотреть"),
    BOOKMARKS("Закладки"),
    HISTORY("История"),
}

/** Действия сетки одним объектом: навигация экрана + события модели, иначе LongParameterList. */
private data class MineActions(
    val onOpenItem: (Int) -> Unit,
    val onOpenCatalog: () -> Unit,
    val onOpenFolder: (BookmarkFolder) -> Unit,
    val onLoadMoreFolderItems: () -> Unit,
    /** Открыть диалог создания папки. */
    val onNewFolder: () -> Unit,
    /** Попросить подтверждение удаления папки (жест — долгое нажатие на плитку). */
    val onDeleteFolder: (BookmarkFolder) -> Unit,
    /** Попросить подтверждение, что тайтл убирают из открытой папки. */
    val onRemoveItem: (Item) -> Unit,
)

/**
 * Состояние диалогов закладок. Живёт в [LibraryScreen], читается [BookmarkDialogHost]. Отдельный
 * держатель, а не три `mutableStateOf` в сигнатуре: так плитки шлют «намерение» одним вызовом,
 * а сам диалог и его подтверждение собраны в одном месте.
 */
@Stable
internal class BookmarkDialogs {
    var creating by mutableStateOf(false)
    var folderToDelete by mutableStateOf<BookmarkFolder?>(null)
    var itemToRemove by mutableStateOf<Item?>(null)
}

/**
 * Раздел «Моё» (экран 08 макета). Все карточки — включая «Продолжить» и «Историю» — ведут в
 * карточку тайтла через [onOpenItem]: там и «Продолжить · SxEy», и выбор серий, и описание.
 */
@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    screenModel: LibraryScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val state by screenModel.collectAsState()
    // Все карточки раздела ведут в карточку тайтла — играть оттуда: «Продолжить · SxEy».
    val onOpenItem: (Int) -> Unit = { itemId -> navigator.open(Destination.Details(itemId)) }
    // Единственное действие пустых состояний раздела.
    val onOpenCatalog: () -> Unit = { navigator.open(Destination.Catalog) }
    var segment by rememberSaveable { mutableStateOf(MineSegment.CONTINUE) }
    val dialogs = remember { BookmarkDialogs() }

    // Внутри папки системная «назад» возвращает к списку папок, а не выкидывает из раздела.
    BackHandler(enabled = state.openFolder != null) {
        screenModel.dispatch(LibraryEvent.CloseFolder)
    }

    BookmarkDialogHost(
        dialogs = dialogs,
        openFolderId = state.openFolder?.folder?.id,
        dispatch = screenModel::dispatch,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding(),
    ) {
        MineHeader(
            segment = segment,
            onSegment = { next ->
                segment = next
                // Уход из «Закладок» закрывает открытую папку: иначе возврат в сегмент показал бы
                // содержимое, которое уже никто не просил.
                if (state.openFolder != null) screenModel.dispatch(LibraryEvent.CloseFolder)
            },
        )

        val openFolder = state.openFolder
        if (segment == MineSegment.BOOKMARKS && openFolder != null) {
            OpenFolderBar(
                folder = openFolder.folder,
                onBack = { screenModel.dispatch(LibraryEvent.CloseFolder) },
            )
        }

        if (state.loading) {
            LoadingBox(Modifier.fillMaxSize())
        } else {
            MineGrid(
                state = state,
                segment = segment,
                actions = MineActions(
                    onOpenItem = onOpenItem,
                    onOpenCatalog = onOpenCatalog,
                    onOpenFolder = { folder -> screenModel.dispatch(LibraryEvent.OpenFolder(folder)) },
                    onLoadMoreFolderItems = { screenModel.dispatch(LibraryEvent.LoadMoreFolderItems) },
                    onNewFolder = { dialogs.creating = true },
                    onDeleteFolder = { folder -> dialogs.folderToDelete = folder },
                    onRemoveItem = { item -> dialogs.itemToRemove = item },
                ),
            )
        }
    }
}

@Composable
private fun MineGrid(state: LibraryState, segment: MineSegment, actions: MineActions) {
    val gridState = rememberLazyGridState()
    val openFolder = state.openFolder

    // Догрузка следующей страницы папки: страниц у kino.pub может быть много, а счётчик на
    // плитке обещает всё содержимое — значит, до конца должно доскроллиться.
    val loadMore by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - LOAD_MORE_TAIL
        }
    }
    LaunchedEffect(loadMore, openFolder?.folder?.id) {
        if (loadMore && openFolder != null) actions.onLoadMoreFolderItems()
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columnsFor(segment, openFolder != null)),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(FilmaxMetrics.CardGap),
        verticalArrangement = Arrangement.spacedBy(FilmaxMetrics.CardGap),
        contentPadding = GridPadding,
    ) {
        when (segment) {
            MineSegment.CONTINUE -> continueSegment(state.history, actions)
            MineSegment.WATCHLIST -> watchlistSegment(state, actions)
            MineSegment.BOOKMARKS -> bookmarksSegment(state, actions)
            MineSegment.HISTORY -> historySegment(state, actions)
        }
    }
}

/** «Продолжить» — начатое, но не досмотренное. Ведёт сразу в плеер, минуя детали. */
private fun LazyGridScope.continueSegment(history: List<WatchHistory>, actions: MineActions) {
    val started = history.filter { entry ->
        val fraction = entry.progress?.fraction ?: 0f
        fraction > CONTINUE_MIN_FRACTION && fraction < CONTINUE_MAX_FRACTION
    }
    if (started.isEmpty()) {
        emptyItem(
            MineEmptySpec(
                icon = Icons.Filled.PlayCircleOutline,
                title = "Ничего не начато",
                hint = "Включите тайтл — он появится здесь с того места, где вы остановились",
                onOpenCatalog = actions.onOpenCatalog,
            ),
        )
        return
    }
    items(started, key = { it.itemId }) { entry -> ProgressCard(entry = entry, onOpenItem = actions.onOpenItem) }
}

/**
 * «Буду смотреть» — отложенное. Один список: локальное избранное и есть кэш серверного
 * watchlist (сервер отдаёт только тоггл и флаг на самом тайтле, списком — никогда).
 */
private fun LazyGridScope.watchlistSegment(state: LibraryState, actions: MineActions) {
    if (state.favorites.isEmpty()) {
        emptyItem(
            MineEmptySpec(
                icon = Icons.Filled.Add,
                title = "Список пуст",
                hint = "Добавляйте тайтлы кнопкой «Буду смотреть»",
                onOpenCatalog = actions.onOpenCatalog,
            ),
        )
        return
    }
    items(state.favorites, key = { it.id }) { favorite ->
        FilmaxPosterCard(
            title = favorite.title,
            posterUrl = favorite.posterSmall,
            onClick = { actions.onOpenItem(favorite.id) },
            width = FilmaxMetrics.GridPosterWidth,
            height = FilmaxMetrics.GridPosterHeight,
            meta = posterMeta(type = null, year = favorite.year),
        )
    }
}

/** «Закладки» — серверные папки: список папок либо содержимое открытой, в этом же экране. */
private fun LazyGridScope.bookmarksSegment(state: LibraryState, actions: MineActions) {
    val openFolder = state.openFolder
    when {
        openFolder != null -> folderItems(openFolder, actions)

        // Папок нет — сразу зовём создать: это единственное осмысленное действие на пустом экране.
        state.lists.isEmpty() -> emptyItem(
            MineEmptySpec(
                icon = Icons.Filled.Folder,
                title = "Папок нет",
                hint = "Создайте папку и собирайте в неё то, к чему вернётесь",
                actionLabel = "Новая папка",
                onAction = actions.onNewFolder,
            ),
        )

        else -> {
            item(key = "new_folder") { NewFolderTile(onClick = actions.onNewFolder) }
            items(state.lists, key = { it.id }) { folder ->
                FolderTile(
                    folder = folder,
                    onClick = { actions.onOpenFolder(folder) },
                    onLongClick = { actions.onDeleteFolder(folder) },
                )
            }
        }
    }
}

private fun LazyGridScope.folderItems(openFolder: OpenBookmarkFolder, actions: MineActions) {
    when {
        openFolder.loading -> item(key = "folder_loading", span = { GridItemSpan(maxLineSpan) }) {
            LoadingBox()
        }

        // Ошибку от пустой папки отличаем: «пусто» и «не загрузилось» требуют разных действий.
        openFolder.items.isEmpty() && openFolder.error != null -> emptyItem(
            MineEmptySpec(
                icon = Icons.Filled.CloudOff,
                title = "Папка не открылась",
                hint = "Вернитесь к списку папок и откройте её ещё раз",
            ),
        )

        openFolder.items.isEmpty() -> emptyItem(
            MineEmptySpec(
                icon = Icons.Filled.Folder,
                title = "Папка пуста",
                hint = "Тайтлы, добавленные в эту папку, появятся здесь",
            ),
        )

        else -> {
            items(openFolder.items, key = { it.id }) { folderItem ->
                FolderItemCard(
                    item = folderItem,
                    onOpen = { actions.onOpenItem(folderItem.id) },
                    onRemove = { actions.onRemoveItem(folderItem) },
                )
            }
            if (openFolder.loadingMore) {
                item(key = "folder_loading_more", span = { GridItemSpan(maxLineSpan) }) { LoadingBox() }
            }
        }
    }
}

/**
 * «История» — всё просмотренное, целиком. Чипа «Скрыть историю» здесь нет намеренно: он живёт
 * на TV, потому что телевизор смотрит вся семья и список «что я смотрел» на нём публичен.
 * Телефон личный и заперт кодом — прятать историю от самого себя не от кого.
 */
private fun LazyGridScope.historySegment(state: LibraryState, actions: MineActions) {
    if (state.history.isEmpty()) {
        emptyItem(
            MineEmptySpec(
                icon = Icons.Filled.History,
                title = "История пуста",
                hint = "Здесь появится всё, что вы смотрели",
                onOpenCatalog = actions.onOpenCatalog,
            ),
        )
        return
    }
    items(state.history, key = { it.itemId }) { entry ->
        ProgressCard(entry = entry, onOpenItem = actions.onOpenItem)
    }
}

/** Колонки при ширине кадра 360dp: карточки 16:9 150dp — вдвое, постеры 98dp — втрое. */
private fun columnsFor(segment: MineSegment, folderOpen: Boolean): Int = when (segment) {
    MineSegment.CONTINUE, MineSegment.HISTORY -> WIDE_COLUMNS
    MineSegment.WATCHLIST -> POSTER_COLUMNS
    MineSegment.BOOKMARKS -> if (folderOpen) POSTER_COLUMNS else FOLDER_COLUMNS
}

private val GridPadding = PaddingValues(
    start = FilmaxMetrics.ScreenPadding,
    end = FilmaxMetrics.ScreenPadding,
    top = 18.dp,
    bottom = 24.dp,
)

private const val POSTER_COLUMNS = 3
private const val WIDE_COLUMNS = 2
private const val FOLDER_COLUMNS = 2

/** Доля просмотра, при которой тайтл считается начатым, но не досмотренным. */
private const val CONTINUE_MIN_FRACTION = 0.01f
private const val CONTINUE_MAX_FRACTION = 0.95f

/** За сколько карточек до конца сетки просить следующую страницу папки (примерно ряд). */
private const val LOAD_MORE_TAIL = 4

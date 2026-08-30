package com.filmax.feature.library.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.user.model.BookmarkFolder
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.tv.designsystem.ScrollToTopOnNavFocus
import com.filmax.core.tv.designsystem.TvButton
import com.filmax.core.tv.designsystem.TvChip
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOnSurface
import com.filmax.core.tv.designsystem.TvOnSurfaceVariant
import com.filmax.core.tv.designsystem.TvOutlineVariant
import com.filmax.core.tv.designsystem.TvPosterCard
import com.filmax.core.tv.designsystem.TvScreenFocus
import com.filmax.core.tv.designsystem.TvSurface
import com.filmax.core.tv.designsystem.TvSurfaceContainerHighest
import com.filmax.core.tv.designsystem.rememberTvScreenFocus
import com.filmax.core.ui.components.posterMeta
import com.filmax.core.ui.components.ratingLabel
import com.filmax.feature.library.common.LibraryEvent
import com.filmax.feature.library.common.LibraryScreenModel
import com.filmax.feature.library.common.LibraryState
import com.filmax.feature.library.common.OpenBookmarkFolder
import org.koin.androidx.compose.koinViewModel

/**
 * Сегменты раздела «Моё» — четыре непересекающихся ответа на вопрос «что у меня есть».
 *
 * Заменяют четыре старых таба Библиотеки, которые пересекались: «Избранное» и серверный
 * watchlist — это одно и то же («Буду смотреть»), а «Загрузки» ничего не качали и убраны.
 * Сегменты живут здесь, а не в общем `LibraryTab`: на телефоне вкладки другие.
 */
private enum class MineSegment(val label: String) {
    CONTINUE("Продолжить"),
    WATCHLIST("Буду смотреть"),
    BOOKMARKS("Закладки"),
    HISTORY("История"),
}

/** Действия раздела одним объектом — как TvHomeActions на главной. */
private data class TvLibraryActions(
    val onOpenItem: (Int) -> Unit,
    val onOpenFolder: (BookmarkFolder) -> Unit,
    val onLoadMoreFolderItems: () -> Unit,
)

/**
 * UI-состояние закладок на TV: активный диалог и «режим удаления» открытой папки. Пульт не знает
 * ни долгих нажатий, ни свайпов, поэтому и создание, и удаление вынесены в явные фокусируемые
 * элементы; это состояние их связывает. Живёт в [TvLibraryScreen], меняется плитками и диалогами.
 */
@Stable
internal class TvBookmarkUi {
    var creating by mutableStateOf(false)
    var folderToDelete by mutableStateOf<BookmarkFolder?>(null)
    var itemToRemove by mutableStateOf<Item?>(null)

    /** В режиме удаления клик по карточке убирает тайтл из папки, а не открывает его. */
    var removeMode by mutableStateOf(false)
}

/**
 * Раздел «Моё» (экран 04 макета) поверх общего [LibraryScreenModel] — данные те же, что и на
 * телефоне. Верхний таб-бар рисует TV-скаффолд в `:app`, фокус и скролл — нативные.
 */
@Composable
fun TvLibraryScreen(
    onOpenItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
    screenModel: LibraryScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()
    var segment by rememberSaveable { mutableStateOf(MineSegment.CONTINUE) }
    val ui = remember { TvBookmarkUi() }

    // Смена или закрытие папки сбрасывает режим удаления: он относится к конкретной открытой папке.
    LaunchedEffect(state.openFolder?.folder?.id) { ui.removeMode = false }

    // Внутри папки «Назад» возвращает к списку папок, а не выкидывает из раздела.
    BackHandler(enabled = state.openFolder != null) {
        screenModel.dispatch(LibraryEvent.CloseFolder)
    }

    TvBookmarkDialogHost(
        ui = ui,
        openFolderId = state.openFolder?.folder?.id,
        dispatch = screenModel::dispatch,
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TvSurface),
    ) {
        MineHeader(
            state = state,
            segment = segment,
            ui = ui,
            onSegment = { next ->
                segment = next
                // Уход из «Закладок» (как и повторное нажатие на них) закрывает открытую папку:
                // иначе возврат в сегмент показал бы содержимое, которое уже никто не просил.
                if (state.openFolder != null) screenModel.dispatch(LibraryEvent.CloseFolder)
            },
            onToggleHistoryHidden = { screenModel.dispatch(LibraryEvent.ToggleHistoryHidden) },
        )

        if (state.loading) {
            LoadingBox(Modifier.fillMaxSize())
        } else {
            MineGrid(
                state = state,
                segment = segment,
                ui = ui,
                actions = TvLibraryActions(
                    onOpenItem = onOpenItem,
                    onOpenFolder = { folder -> screenModel.dispatch(LibraryEvent.OpenFolder(folder)) },
                    onLoadMoreFolderItems = { screenModel.dispatch(LibraryEvent.LoadMoreFolderItems) },
                ),
            )
        }
    }
}

/**
 * Шапка раздела: заголовок, сегменты и подстрока текущего сегмента. Не скроллится вместе с
 * сеткой — таб-бар в скаффолде прозрачный, и уезжающий под него контент налезал бы на вкладки.
 */
@Composable
private fun MineHeader(
    state: LibraryState,
    segment: MineSegment,
    ui: TvBookmarkUi,
    onSegment: (MineSegment) -> Unit,
    onToggleHistoryHidden: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(
                start = TvMetrics.SafeHorizontal,
                end = TvMetrics.SafeHorizontal,
                top = TvMetrics.ContentTop,
            ),
    ) {
        Text("Моё", style = MaterialTheme.typography.headlineMedium, color = TvOnSurface)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MineSegment.entries.forEach { entry ->
                TvChip(
                    label = entry.label,
                    selected = entry == segment,
                    onClick = { onSegment(entry) },
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(thickness = 1.dp, color = TvOutlineVariant)

        val openFolder = state.openFolder
        when {
            segment == MineSegment.BOOKMARKS && openFolder != null ->
                OpenFolderBar(folder = openFolder.folder, ui = ui)
            segment == MineSegment.HISTORY -> HistoryPrivacyChip(
                hidden = state.historyHidden,
                onToggle = onToggleHistoryHidden,
            )
            else -> Unit
        }
    }
}

/**
 * Чип приватности истории. Телевизор смотрит вся семья, и «что я смотрел» на нём публично —
 * чип убирает список с экрана. Скрытие живёт до перезапуска приложения (хранить флаг негде),
 * и пустое состояние сегмента об этом честно говорит.
 */
@Composable
private fun HistoryPrivacyChip(hidden: Boolean, onToggle: () -> Unit) {
    Row(Modifier.padding(top = 18.dp)) {
        TvChip(
            label = if (hidden) "Показать историю" else "Скрыть историю",
            selected = hidden,
            onClick = onToggle,
        )
    }
}

/**
 * Панель открытой папки: заголовок и действия над папкой. Оба действия — фокусируемые кнопки, а
 * не жесты: пультом до них доезжают вверх от сетки. «Убрать тайтлы» переводит папку в режим
 * удаления (клик по карточке убирает её), «Удалить папку» просит подтверждение. «Назад» — к списку.
 */
@Composable
private fun OpenFolderBar(folder: BookmarkFolder, ui: TvBookmarkUi) {
    Row(
        modifier = Modifier.padding(top = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            folder.title,
            style = MaterialTheme.typography.titleMedium,
            color = TvOnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        TvChip(
            label = if (ui.removeMode) "Готово" else "Убрать тайтлы",
            selected = ui.removeMode,
            onClick = { ui.removeMode = !ui.removeMode },
        )
        TvButton(
            text = "Удалить папку",
            onClick = { ui.folderToDelete = folder },
            primary = false,
        )
    }
}

@Composable
private fun MineGrid(
    state: LibraryState,
    segment: MineSegment,
    ui: TvBookmarkUi,
    actions: TvLibraryActions,
) {
    val gridState = rememberLazyGridState()
    ScrollToTopOnNavFocus(gridState)
    val focus = rememberTvScreenFocus()
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

    // Сегмент и открытая папка меняют содержимое сетки целиком: плитка, на которой стоял
    // фокус, уходит из композиции, и без сброса фокус повис бы — пульт перестал бы отвечать.
    LaunchedEffect(segment, openFolder?.folder?.id) { focus.focusOn() }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columnsFor(segment, openFolder != null)),
        state = gridState,
        modifier = Modifier.fillMaxSize().then(focus.containerModifier),
        horizontalArrangement = Arrangement.spacedBy(TvMetrics.CardGap),
        verticalArrangement = Arrangement.spacedBy(TvMetrics.RowGap),
        contentPadding = GridPadding,
    ) {
        when (segment) {
            MineSegment.CONTINUE -> continueSegment(state.history, actions.onOpenItem, focus)
            MineSegment.WATCHLIST -> watchlistSegment(state, actions.onOpenItem, focus)
            MineSegment.BOOKMARKS -> bookmarksSegment(state, ui, actions, focus)
            MineSegment.HISTORY -> historySegment(state, actions.onOpenItem, focus)
        }
    }
}

/** «Продолжить» — начатое, но не досмотренное. Ведёт в карточку тайтла: там и «продолжить», и контекст. */
private fun LazyGridScope.continueSegment(
    history: List<WatchHistory>,
    onOpenItem: (Int) -> Unit,
    focus: TvScreenFocus,
) {
    val started = history.filter { entry ->
        val fraction = entry.progress?.fraction ?: 0f
        fraction > CONTINUE_MIN_FRACTION && fraction < CONTINUE_MAX_FRACTION
    }
    if (started.isEmpty()) {
        emptyItem(
            icon = Icons.Filled.PlayCircleOutline,
            title = "Ничего не начато",
            hint = "Включите тайтл — он появится здесь с того места, где вы остановились",
        )
        return
    }
    items(started, key = { it.itemId }) { entry ->
        ProgressCard(
            entry = entry,
            returnKey = "continue:${entry.itemId}",
            focus = focus,
            onOpenItem = onOpenItem,
        )
    }
}

/**
 * «Буду смотреть» — отложенное. Один список: локальное избранное и есть кэш серверного
 * watchlist (сервер отдаёт только тоггл и флаг на самом тайтле, списком — никогда).
 */
private fun LazyGridScope.watchlistSegment(
    state: LibraryState,
    onOpenItem: (Int) -> Unit,
    focus: TvScreenFocus,
) {
    if (state.favorites.isEmpty()) {
        emptyItem(
            icon = Icons.Filled.Add,
            title = "Список пуст",
            hint = "Добавляйте тайтлы кнопкой «Буду смотреть»",
        )
        return
    }
    items(state.favorites, key = { it.id }) { item ->
        TvPosterCard(
            title = item.title,
            meta = posterMeta(type = null, year = item.year),
            posterUrl = item.posterSmall,
            onClick = { onOpenItem(item.id) },
            modifier = focus.item("watchlist:${item.id}"),
            posterContent = { url, posterModifier ->
                TvPoster(url, item.title, posterModifier, TvMetrics.PosterShape)
            },
        )
    }
}

/** «Закладки» — серверные папки: список папок либо содержимое открытой. */
private fun LazyGridScope.bookmarksSegment(
    state: LibraryState,
    ui: TvBookmarkUi,
    actions: TvLibraryActions,
    focus: TvScreenFocus,
) {
    val openFolder = state.openFolder
    if (openFolder == null) {
        folderTiles(state.lists, actions.onOpenFolder, onNewFolder = { ui.creating = true })
    } else {
        folderItems(openFolder, ui, actions.onOpenItem, focus)
    }
}

private fun LazyGridScope.folderTiles(
    folders: List<BookmarkFolder>,
    onOpenFolder: (BookmarkFolder) -> Unit,
    onNewFolder: () -> Unit,
) {
    if (folders.isEmpty()) {
        // Пусто — сразу зовём создать: единственное осмысленное действие, и кнопка сама берёт фокус.
        item(key = "empty", span = { GridItemSpan(maxLineSpan) }) { BookmarksEmpty(onNewFolder) }
        return
    }
    item(key = "new_folder") { NewFolderTile(onClick = onNewFolder) }
    items(folders, key = { it.id }) { folder ->
        FolderTile(folder = folder, onClick = { onOpenFolder(folder) })
    }
}

private fun LazyGridScope.folderItems(
    openFolder: OpenBookmarkFolder,
    ui: TvBookmarkUi,
    onOpenItem: (Int) -> Unit,
    focus: TvScreenFocus,
) {
    when {
        openFolder.loading ->
            item(key = "folder_loading", span = { GridItemSpan(maxLineSpan) }) { LoadingBox() }

        // Ошибку от пустой папки отличаем: «пусто» и «не загрузилось» требуют разных действий.
        openFolder.items.isEmpty() && openFolder.error != null -> emptyItem(
            icon = Icons.Filled.CloudOff,
            title = "Папка не открылась",
            hint = "Нажмите «Назад» и откройте её ещё раз",
        )

        openFolder.items.isEmpty() -> emptyItem(
            icon = Icons.Filled.Folder,
            title = "Папка пуста",
            hint = "Тайтлы, добавленные в эту папку, появятся здесь",
        )

        else -> folderPosters(openFolder = openFolder, ui = ui, onOpenItem = onOpenItem, focus = focus)
    }
}

private fun LazyGridScope.folderPosters(
    openFolder: OpenBookmarkFolder,
    ui: TvBookmarkUi,
    onOpenItem: (Int) -> Unit,
    focus: TvScreenFocus,
) {
    items(openFolder.items, key = { it.id }) { item ->
        TvPosterCard(
            title = item.title,
            meta = posterMeta(type = item.genres.firstOrNull()?.title, year = item.year),
            posterUrl = item.posters.medium.ifBlank { item.posters.small },
            // В режиме удаления карточка убирает тайтл (по подтверждению), иначе открывает детали.
            onClick = {
                if (ui.removeMode) {
                    ui.itemToRemove = item
                } else {
                    onOpenItem(item.id)
                }
            },
            modifier = focus.item("folder:${item.id}"),
            rating = ratingLabel(item.rating.external),
            posterContent = { url, posterModifier ->
                FolderPoster(url, item.title, posterModifier, removeMode = ui.removeMode)
            },
        )
    }
    if (openFolder.loadingMore) {
        item(key = "folder_loading_more", span = { GridItemSpan(maxLineSpan) }) { LoadingBox() }
    }
}

/** «История» — всё просмотренное. Как и «Продолжить», ведёт в карточку тайтла. */
private fun LazyGridScope.historySegment(
    state: LibraryState,
    onOpenItem: (Int) -> Unit,
    focus: TvScreenFocus,
) {
    if (state.historyHidden) {
        emptyItem(
            icon = Icons.Filled.VisibilityOff,
            title = "История скрыта",
            hint = "Вернётся по тому же чипу или после перезапуска приложения",
        )
        return
    }
    if (state.history.isEmpty()) {
        emptyItem(
            icon = Icons.Filled.History,
            title = "История пуста",
            hint = "Здесь появится всё, что вы смотрели",
        )
        return
    }
    items(state.history, key = { it.itemId }) { entry ->
        ProgressCard(
            entry = entry,
            returnKey = "history:${entry.itemId}",
            focus = focus,
            onOpenItem = onOpenItem,
        )
    }
}

private fun LazyGridScope.emptyItem(icon: ImageVector, title: String, hint: String) {
    item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
        MineEmpty(icon = icon, title = title, hint = hint)
    }
}

@Composable
private fun MineEmpty(icon: ImageVector, title: String, hint: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TvSurfaceContainerHighest,
            modifier = Modifier.size(36.dp),
        )
        Text(title, style = MaterialTheme.typography.titleLarge, color = TvOnSurface)
        Text(hint, style = MaterialTheme.typography.bodyLarge, color = TvOnSurfaceVariant)
    }
}

/** Колонки сетки при ширине макета 960dp: постеры 190dp — вчетверо, карточки 16:9 250dp — втрое. */
private fun columnsFor(segment: MineSegment, folderOpen: Boolean): Int = when (segment) {
    MineSegment.CONTINUE, MineSegment.HISTORY -> WIDE_COLUMNS
    MineSegment.WATCHLIST -> POSTER_COLUMNS
    MineSegment.BOOKMARKS -> if (folderOpen) POSTER_COLUMNS else FOLDER_COLUMNS
}

/**
 * Отступы сетки. Боковые поля живут только здесь: на родителе они срезали бы рамку фокуса
 * (карточка при фокусе растёт), а contentPadding сетка не клипает. Сверху и снизу — запас
 * ровно под это увеличение.
 */
private val GridPadding = PaddingValues(
    start = TvMetrics.SafeHorizontal,
    end = TvMetrics.SafeHorizontal,
    top = TvMetrics.FocusInset,
    bottom = TvMetrics.FocusInset + TvMetrics.SafeVertical,
)

internal val FolderTileHeight = 130.dp

/** Ширина диалогов закладок: у́же экрана, чтобы читаться с дивана и не растягивать кнопки. */
internal val DialogMaxWidth = 420.dp

private const val POSTER_COLUMNS = 4
private const val WIDE_COLUMNS = 3
private const val FOLDER_COLUMNS = 3

/** Фильм — единственный трек, эпизод выбирать не из чего: PlayerRoute.videoId = -1. */

/** Доля просмотра, при которой тайтл считается начатым, но не досмотренным. */
private const val CONTINUE_MIN_FRACTION = 0.01f
private const val CONTINUE_MAX_FRACTION = 0.95f

/** За сколько карточек до конца сетки просить следующую страницу папки (примерно ряд). */
private const val LOAD_MORE_TAIL = 4

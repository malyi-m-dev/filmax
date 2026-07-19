package com.filmax.feature.library.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.ShapeButton
import com.filmax.core.designsystem.ShapeCard
import com.filmax.core.designsystem.ShapeFull
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.user.model.BookmarkFolder
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.domain.watching.model.WatchProgress
import com.filmax.core.ui.components.FilmaxEmptyState
import com.filmax.core.ui.components.FilmaxPosterCard
import com.filmax.core.ui.components.FilmaxProgressCard
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
 * Заменяют старые вкладки Библиотеки, которые пересекались: «Избранное» и есть «Буду смотреть»,
 * а «Загрузки» ничего не качали и убраны. Совпадают с сегментами TV-раздела, но живут отдельно:
 * общий `LibraryTab` описывал именно старые вкладки и мобильному экрану больше не нужен.
 */
private enum class MineSegment(val label: String) {
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
private class BookmarkDialogs {
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
    onOpenItem: (Int) -> Unit,
    onOpenCatalog: () -> Unit,
    modifier: Modifier = Modifier,
    screenModel: LibraryScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()
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

// ── Шапка ─────────────────────────────────────────────────────────────────

/** Заголовок и сегменты. Не скроллятся вместе с сеткой: сегмент всегда должен быть виден. */
@Composable
private fun MineHeader(segment: MineSegment, onSegment: (MineSegment) -> Unit) {
    Column {
        Text(
            "Моё",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(horizontal = FilmaxMetrics.ScreenPadding)
                .padding(top = 6.dp),
        )
        LazyRow(
            modifier = Modifier.padding(top = 16.dp, bottom = 14.dp),
            contentPadding = PaddingValues(horizontal = FilmaxMetrics.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(MineSegment.entries, key = { it.name }) { entry ->
                MineChip(
                    label = entry.label,
                    selected = entry == segment,
                    onClick = { onSegment(entry) },
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** Чип-сегмент: выбранный — единственная белая заливка на экране, остальные без обводки и фона. */
@Composable
private fun MineChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(FilmaxMetrics.ChipHeight)
            .clip(ShapeFull)
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/** Строка открытой папки: где мы и чем отсюда выйти — жестом «назад» или тапом по этой строке. */
@Composable
private fun OpenFolderBar(folder: BookmarkFolder, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBack)
            .padding(horizontal = FilmaxMetrics.ScreenPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "К списку папок",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            folder.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── Сетка ─────────────────────────────────────────────────────────────────

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

// ── Карточки ──────────────────────────────────────────────────────────────

@Composable
private fun ProgressCard(entry: WatchHistory, onOpenItem: (Int) -> Unit) {
    FilmaxProgressCard(
        title = entry.title,
        meta = progressMeta(entry.progress),
        // Карточка 16:9 — берём кадр серии, а не вертикальный постер: тот обрезался бы по центру.
        posterUrl = entry.wideOrPoster,
        progress = entry.progress?.fraction ?: 0f,
        // В карточку тайтла, а не сразу в плеер: там «Продолжить · SxEy», серии и описание.
        onClick = { onOpenItem(entry.itemId) },
        width = FilmaxMetrics.MineCardWidth,
        height = FilmaxMetrics.MineCardHeight,
    )
}

/**
 * Плитка папки-закладки. Тап открывает содержимое (грузит [LibraryScreenModel]), долгое нажатие —
 * запрос на удаление: отдельной кнопки на плитке нет, чтобы не зашумлять сетку.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderTile(folder: BookmarkFolder, onClick: () -> Unit, onLongClick: () -> Unit) {
    val count = folder.count
    val word = when {
        count % 100 in 11..14 -> "тайтлов"
        count % 10 == 1 -> "тайтл"
        count % 10 in 2..4 -> "тайтла"
        else -> "тайтлов"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(FolderTileHeight)
            .clip(ShapeCard)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            folder.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "$count $word",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

/** Плитка «＋ Новая папка» — первая ячейка сетки папок и вход в диалог создания. */
@Composable
private fun NewFolderTile(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(FolderTileHeight)
            .clip(ShapeCard)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Новая папка",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Карточка тайтла внутри папки. Поверх постера — маленький крестик «убрать»: у [FilmaxPosterCard]
 * нет долгого нажатия, а явная иконка заметнее жеста. Крестик слева, чтобы не спорить с пилюлей
 * рейтинга справа.
 */
@Composable
private fun FolderItemCard(item: Item, onOpen: () -> Unit, onRemove: () -> Unit) {
    Box {
        FilmaxPosterCard(
            title = item.title,
            posterUrl = item.posters.medium.ifBlank { item.posters.small },
            onClick = onOpen,
            width = FilmaxMetrics.GridPosterWidth,
            height = FilmaxMetrics.GridPosterHeight,
            rating = ratingLabel(item.rating.external),
            meta = posterMeta(type = null, year = item.year),
        )
        RemoveBadge(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(5.dp),
        )
    }
}

/** Круглый крестик поверх постера: убирает тайтл из папки (по подтверждению). */
@Composable
private fun RemoveBadge(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Close,
            contentDescription = "Убрать из папки",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(14.dp),
        )
    }
}

// ── Пустые состояния и загрузка ───────────────────────────────────────────

/**
 * Содержимое пустого сегмента. «Открыть каталог» — там, где каталог и есть ответ; [onAction] с
 * [actionLabel] — главное действие сегмента (для «Закладок» это «Новая папка»).
 */
private data class MineEmptySpec(
    val icon: ImageVector,
    val title: String,
    val hint: String,
    val onOpenCatalog: (() -> Unit)? = null,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)

/** Пустое состояние занимает всю ширину сетки, а не одну колонку. */
private fun LazyGridScope.emptyItem(spec: MineEmptySpec) {
    item(key = "empty", span = { GridItemSpan(maxLineSpan) }) { MineEmpty(spec) }
}

@Composable
private fun MineEmpty(spec: MineEmptySpec) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FilmaxEmptyState(icon = spec.icon, title = spec.title, subtitle = spec.hint)
        val onAction = spec.onAction
        val actionLabel = spec.actionLabel
        if (onAction != null && actionLabel != null) {
            Spacer(Modifier.height(18.dp))
            PrimaryActionButton(label = actionLabel, onClick = onAction)
        }
        val onOpenCatalog = spec.onOpenCatalog
        if (onOpenCatalog != null) {
            Spacer(Modifier.height(18.dp))
            OpenCatalogButton(onClick = onOpenCatalog)
        }
    }
}

/** Главная кнопка сегмента: единственная белая заливка на пустом экране (создать папку). */
@Composable
private fun PrimaryActionButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(FilmaxMetrics.PrimaryButtonHeight)
            .clip(ShapeButton)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

/** Вторичная кнопка: в монохроме белая заливка зарезервирована за главным действием экрана. */
@Composable
private fun OpenCatalogButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(FilmaxMetrics.SecondaryButtonHeight)
            .clip(ShapeButton)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Открыть каталог",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── Диалоги закладок ──────────────────────────────────────────────────────

/** Рисует активный диалог закладок и переводит подтверждение в события [LibraryScreenModel]. */
@Composable
private fun BookmarkDialogHost(
    dialogs: BookmarkDialogs,
    openFolderId: Int?,
    dispatch: (LibraryEvent) -> Unit,
) {
    if (dialogs.creating) {
        CreateFolderDialog(
            onDismiss = { dialogs.creating = false },
            onConfirm = { name ->
                dispatch(LibraryEvent.CreateFolder(name))
                dialogs.creating = false
            },
        )
    }
    dialogs.folderToDelete?.let { folder ->
        ConfirmActionDialog(
            title = "Удалить папку?",
            message = "«${folder.title}» и её список исчезнут. Сами тайтлы останутся в каталоге.",
            confirmLabel = "Удалить",
            onConfirm = {
                dispatch(LibraryEvent.DeleteFolder(folder.id))
                dialogs.folderToDelete = null
            },
            onDismiss = { dialogs.folderToDelete = null },
        )
    }
    dialogs.itemToRemove?.let { item ->
        ConfirmActionDialog(
            title = "Убрать из папки?",
            message = "«${item.title}» исчезнет из этой папки, но останется в каталоге.",
            confirmLabel = "Убрать",
            onConfirm = {
                // openFolderId непустой, пока папка открыта; на всякий случай не шлём событие без него.
                openFolderId?.let { folderId ->
                    dispatch(LibraryEvent.RemoveItemFromFolder(item.id, folderId))
                }
                dialogs.itemToRemove = null
            },
            onDismiss = { dialogs.itemToRemove = null },
        )
    }
}

/** Диалог ввода имени новой папки. Поле получает фокус сразу — клавиатура открывается без лишнего тапа. */
@Composable
private fun CreateFolderDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text("Создать")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        title = { Text("Новая папка", style = MaterialTheme.typography.titleLarge) },
        text = {
            FolderNameField(value = name, onValueChange = { name = it }, focusRequester = focusRequester)
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
    )
}

/** Поле имени папки в стиле поиска: pill-контейнер с [BasicTextField] и плейсхолдером. */
@Composable
private fun FolderNameField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(FilmaxMetrics.SearchFieldHeight)
            .clip(ShapeButton)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    "Название папки",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        }
    }
}

/** Диалог подтверждения деструктивного действия (удалить папку / убрать тайтл). */
@Composable
private fun ConfirmActionDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun LoadingBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

// ── Размеры и форматирование ──────────────────────────────────────────────

/** Колонки при ширине кадра 360dp: карточки 16:9 150dp — вдвое, постеры 98dp — втрое. */
private fun columnsFor(segment: MineSegment, folderOpen: Boolean): Int = when (segment) {
    MineSegment.CONTINUE, MineSegment.HISTORY -> WIDE_COLUMNS
    MineSegment.WATCHLIST -> POSTER_COLUMNS
    MineSegment.BOOKMARKS -> if (folderOpen) POSTER_COLUMNS else FOLDER_COLUMNS
}

/**
 * Подпись карточки: «S2 · осталось 18 мин». Номер эпизода макета («E5») не выводим: в
 * [WatchProgress] его нет — `videoId` это идентификатор трека, а не порядковый номер серии.
 */
private fun progressMeta(progress: WatchProgress?): String? {
    if (progress == null) return null
    val parts = buildList {
        progress.season?.takeIf { it > 0 }?.let { season -> add("S$season") }
        remainingLabel(progress)?.let { remaining -> add("осталось $remaining") }
    }
    return parts.joinToString(" · ").ifBlank { null }
}

/** Остаток трека словами; null — прогресса нет или уже досмотрено. */
private fun remainingLabel(progress: WatchProgress): String? =
    remainingMinutes(progress)?.let { minutes ->
        val hours = minutes / MINUTES_IN_HOUR
        val rest = minutes % MINUTES_IN_HOUR
        when {
            hours > 0 && rest > 0 -> "$hours ч $rest мин"
            hours > 0 -> "$hours ч"
            else -> "$rest мин"
        }
    }

/** Сколько минут осталось; null — прогресса нет или трек уже досмотрен. */
private fun remainingMinutes(progress: WatchProgress): Int? {
    val watched = progress.timeSeconds
    val total = progress.durationSeconds?.takeIf { it > 0 }
    if (watched == null || total == null) return null
    return ((total - watched) / SECONDS_IN_MINUTE).takeIf { it > 0 }
}

private val GridPadding = PaddingValues(
    start = FilmaxMetrics.ScreenPadding,
    end = FilmaxMetrics.ScreenPadding,
    top = 18.dp,
    bottom = 24.dp,
)

private val FolderTileHeight = 84.dp

private const val POSTER_COLUMNS = 3
private const val WIDE_COLUMNS = 2
private const val FOLDER_COLUMNS = 2

/** `PlayerRoute.videoId` для фильма/неизвестного эпизода — плеер возьмёт первый трек. */

/** Доля просмотра, при которой тайтл считается начатым, но не досмотренным. */
private const val CONTINUE_MIN_FRACTION = 0.01f
private const val CONTINUE_MAX_FRACTION = 0.95f

private const val MINUTES_IN_HOUR = 60
private const val SECONDS_IN_MINUTE = 60

/** За сколько карточек до конца сетки просить следующую страницу папки (примерно ряд). */
private const val LOAD_MORE_TAIL = 4

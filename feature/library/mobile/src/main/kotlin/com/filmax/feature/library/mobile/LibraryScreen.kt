package com.filmax.feature.library.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.ShapeButton
import com.filmax.core.designsystem.ShapeCard
import com.filmax.core.designsystem.ShapeFull
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
    val onPlay: (itemId: Int, videoId: Int) -> Unit,
    val onOpenCatalog: () -> Unit,
    val onOpenFolder: (BookmarkFolder) -> Unit,
    val onLoadMoreFolderItems: () -> Unit,
)

/**
 * Раздел «Моё» (экран 08 макета). [onPlay] ведёт в плеер: «Продолжить» и «История» — это про
 * воспроизведение, а не про чтение описания. Постеры «Буду смотреть» и содержимое папок —
 * в детали через [onOpenItem].
 */
@Composable
fun LibraryScreen(
    onOpenItem: (Int) -> Unit,
    onPlay: (itemId: Int, videoId: Int) -> Unit,
    onOpenCatalog: () -> Unit,
    modifier: Modifier = Modifier,
    screenModel: LibraryScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()
    var segment by rememberSaveable { mutableStateOf(MineSegment.CONTINUE) }

    // Внутри папки системная «назад» возвращает к списку папок, а не выкидывает из раздела.
    BackHandler(enabled = state.openFolder != null) {
        screenModel.dispatch(LibraryEvent.CloseFolder)
    }

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
                    onPlay = onPlay,
                    onOpenCatalog = onOpenCatalog,
                    onOpenFolder = { folder -> screenModel.dispatch(LibraryEvent.OpenFolder(folder)) },
                    onLoadMoreFolderItems = { screenModel.dispatch(LibraryEvent.LoadMoreFolderItems) },
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
    items(started, key = { it.itemId }) { entry -> ProgressCard(entry = entry, onPlay = actions.onPlay) }
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
        openFolder != null -> folderItems(openFolder, actions.onOpenItem)

        state.lists.isEmpty() -> emptyItem(
            MineEmptySpec(
                icon = Icons.Filled.Folder,
                title = "Папок нет",
                hint = "Папки закладок вашего аккаунта появятся здесь",
            ),
        )

        else -> items(state.lists, key = { it.id }) { folder ->
            FolderTile(folder = folder, onClick = { actions.onOpenFolder(folder) })
        }
    }
}

private fun LazyGridScope.folderItems(openFolder: OpenBookmarkFolder, onOpenItem: (Int) -> Unit) {
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
                FilmaxPosterCard(
                    title = folderItem.title,
                    posterUrl = folderItem.posters.medium.ifBlank { folderItem.posters.small },
                    onClick = { onOpenItem(folderItem.id) },
                    width = FilmaxMetrics.GridPosterWidth,
                    height = FilmaxMetrics.GridPosterHeight,
                    rating = ratingLabel(folderItem.rating.external),
                    meta = posterMeta(type = null, year = folderItem.year),
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
        ProgressCard(entry = entry, onPlay = actions.onPlay)
    }
}

// ── Карточки ──────────────────────────────────────────────────────────────

@Composable
private fun ProgressCard(entry: WatchHistory, onPlay: (itemId: Int, videoId: Int) -> Unit) {
    FilmaxProgressCard(
        title = entry.title,
        meta = progressMeta(entry.progress),
        // Карточка 16:9 — берём кадр серии, а не вертикальный постер: тот обрезался бы по центру.
        posterUrl = entry.wideOrPoster,
        progress = entry.progress?.fraction ?: 0f,
        // Эпизод берём из истории, позицию внутри трека восстановит плеер.
        onClick = { onPlay(entry.itemId, entry.progress?.videoId ?: NO_VIDEO_ID) },
        width = FilmaxMetrics.MineCardWidth,
        height = FilmaxMetrics.MineCardHeight,
    )
}

/** Плитка папки-закладки: тап открывает содержимое в этом же экране — грузит [LibraryScreenModel]. */
@Composable
private fun FolderTile(folder: BookmarkFolder, onClick: () -> Unit) {
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
            .clickable(onClick = onClick)
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

// ── Пустые состояния и загрузка ───────────────────────────────────────────

/** Содержимое пустого сегмента. Кнопка «Открыть каталог» — только там, где каталог и есть ответ. */
private data class MineEmptySpec(
    val icon: ImageVector,
    val title: String,
    val hint: String,
    val onOpenCatalog: (() -> Unit)? = null,
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
        val onOpenCatalog = spec.onOpenCatalog
        if (onOpenCatalog != null) {
            Spacer(Modifier.height(18.dp))
            OpenCatalogButton(onClick = onOpenCatalog)
        }
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
private const val NO_VIDEO_ID = -1

/** Доля просмотра, при которой тайтл считается начатым, но не досмотренным. */
private const val CONTINUE_MIN_FRACTION = 0.01f
private const val CONTINUE_MAX_FRACTION = 0.95f

private const val MINUTES_IN_HOUR = 60
private const val SECONDS_IN_MINUTE = 60

/** За сколько карточек до конца сетки просить следующую страницу папки (примерно ряд). */
private const val LOAD_MORE_TAIL = 4

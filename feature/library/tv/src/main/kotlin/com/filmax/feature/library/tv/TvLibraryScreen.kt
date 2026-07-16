package com.filmax.feature.library.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.domain.user.model.BookmarkFolder
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.domain.watching.model.WatchProgress
import com.filmax.core.tv.designsystem.ScrollToTopOnNavFocus
import com.filmax.core.tv.designsystem.TvAccent
import com.filmax.core.tv.designsystem.TvChip
import com.filmax.core.tv.designsystem.TvFocusCard
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOnSurface
import com.filmax.core.tv.designsystem.TvOnSurfaceDim
import com.filmax.core.tv.designsystem.TvOnSurfaceVariant
import com.filmax.core.tv.designsystem.TvOutlineVariant
import com.filmax.core.tv.designsystem.TvPosterCard
import com.filmax.core.tv.designsystem.TvProgressCard
import com.filmax.core.tv.designsystem.TvSurface
import com.filmax.core.tv.designsystem.TvSurfaceContainer
import com.filmax.core.tv.designsystem.TvSurfaceContainerHighest
import com.filmax.core.tv.designsystem.posterMeta
import com.filmax.core.tv.designsystem.ratingLabel
import com.filmax.core.ui.components.PosterImage
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
    val onPlay: (itemId: Int, videoId: Int) -> Unit,
    val onOpenFolder: (BookmarkFolder) -> Unit,
    val onLoadMoreFolderItems: () -> Unit,
)

/**
 * Раздел «Моё» (экран 04 макета) поверх общего [LibraryScreenModel] — данные те же, что и на
 * телефоне. Верхний таб-бар рисует TV-скаффолд в `:app`, фокус и скролл — нативные.
 */
@Composable
fun TvLibraryScreen(
    onOpenItem: (Int) -> Unit,
    onPlay: (itemId: Int, videoId: Int) -> Unit,
    modifier: Modifier = Modifier,
    screenModel: LibraryScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()
    var segment by rememberSaveable { mutableStateOf(MineSegment.CONTINUE) }

    // Внутри папки «Назад» возвращает к списку папок, а не выкидывает из раздела.
    BackHandler(enabled = state.openFolder != null) {
        screenModel.dispatch(LibraryEvent.CloseFolder)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TvSurface),
    ) {
        MineHeader(
            state = state,
            segment = segment,
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
                actions = TvLibraryActions(
                    onOpenItem = onOpenItem,
                    onPlay = onPlay,
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
            segment == MineSegment.BOOKMARKS && openFolder != null -> OpenFolderCaption(openFolder.folder)
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

/** Подпись открытой папки: где мы и чем отсюда выйти (у пульта нет видимой кнопки «назад»). */
@Composable
private fun OpenFolderCaption(folder: BookmarkFolder) {
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
        )
        Text(
            "«Назад» — к списку папок",
            style = MaterialTheme.typography.bodySmall,
            color = TvOnSurfaceDim,
        )
    }
}

@Composable
private fun MineGrid(state: LibraryState, segment: MineSegment, actions: TvLibraryActions) {
    val gridState = rememberLazyGridState()
    ScrollToTopOnNavFocus(gridState)
    val openFolder = state.openFolder
    val gridFocus = remember { FocusRequester() }

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

    // Плитка папки, на которой был фокус, уходит из композиции при открытии/закрытии папки:
    // без явного запроса фокус повисает и пульт перестаёт отвечать. Карточки сетка композит
    // в фазе измерения, поэтому просим фокус несколько кадров подряд.
    LaunchedEffect(openFolder?.folder?.id, openFolder?.loading) {
        if (segment != MineSegment.BOOKMARKS || openFolder?.loading == true) return@LaunchedEffect
        repeat(FOCUS_ATTEMPTS) {
            withFrameNanos { }
            runCatching { gridFocus.requestFocus() }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columnsFor(segment, openFolder != null)),
        state = gridState,
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(gridFocus)
            .focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(TvMetrics.CardGap),
        verticalArrangement = Arrangement.spacedBy(TvMetrics.RowGap),
        contentPadding = GridPadding,
    ) {
        when (segment) {
            MineSegment.CONTINUE -> continueSegment(state.history, actions.onPlay)
            MineSegment.WATCHLIST -> watchlistSegment(state, actions.onOpenItem)
            MineSegment.BOOKMARKS -> bookmarksSegment(state, actions)
            MineSegment.HISTORY -> historySegment(state, actions.onPlay)
        }
    }
}

/** «Продолжить» — начатое, но не досмотренное. Ведёт сразу в плеер, а не в описание. */
private fun LazyGridScope.continueSegment(
    history: List<WatchHistory>,
    onPlay: (itemId: Int, videoId: Int) -> Unit,
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
    items(started, key = { it.itemId }) { entry -> ProgressCard(entry = entry, onPlay = onPlay) }
}

/**
 * «Буду смотреть» — отложенное. Один список: локальное избранное и есть кэш серверного
 * watchlist (сервер отдаёт только тоггл и флаг на самом тайтле, списком — никогда).
 */
private fun LazyGridScope.watchlistSegment(state: LibraryState, onOpenItem: (Int) -> Unit) {
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
            posterContent = { url, posterModifier ->
                TvPoster(url, item.title, posterModifier, TvMetrics.PosterShape)
            },
        )
    }
}

/** «Закладки» — серверные папки: список папок либо содержимое открытой. */
private fun LazyGridScope.bookmarksSegment(state: LibraryState, actions: TvLibraryActions) {
    val openFolder = state.openFolder
    if (openFolder == null) {
        folderTiles(state.lists, actions.onOpenFolder)
    } else {
        folderItems(openFolder, actions.onOpenItem)
    }
}

private fun LazyGridScope.folderTiles(
    folders: List<BookmarkFolder>,
    onOpenFolder: (BookmarkFolder) -> Unit,
) {
    if (folders.isEmpty()) {
        emptyItem(
            icon = Icons.Filled.Folder,
            title = "Папок нет",
            hint = "Папки закладок вашего аккаунта появятся здесь",
        )
        return
    }
    items(folders, key = { it.id }) { folder ->
        FolderTile(folder = folder, onClick = { onOpenFolder(folder) })
    }
}

private fun LazyGridScope.folderItems(openFolder: OpenBookmarkFolder, onOpenItem: (Int) -> Unit) {
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

        else -> folderPosters(openFolder = openFolder, onOpenItem = onOpenItem)
    }
}

private fun LazyGridScope.folderPosters(openFolder: OpenBookmarkFolder, onOpenItem: (Int) -> Unit) {
    items(openFolder.items, key = { it.id }) { item ->
        TvPosterCard(
            title = item.title,
            meta = posterMeta(type = item.genres.firstOrNull()?.title, year = item.year),
            posterUrl = item.posters.medium.ifBlank { item.posters.small },
            onClick = { onOpenItem(item.id) },
            rating = ratingLabel(item.rating.external),
            posterContent = { url, posterModifier ->
                TvPoster(url, item.title, posterModifier, TvMetrics.PosterShape)
            },
        )
    }
    if (openFolder.loadingMore) {
        item(key = "folder_loading_more", span = { GridItemSpan(maxLineSpan) }) { LoadingBox() }
    }
}

/** «История» — всё просмотренное. Как и «Продолжить», ведёт в плеер. */
private fun LazyGridScope.historySegment(
    state: LibraryState,
    onPlay: (itemId: Int, videoId: Int) -> Unit,
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
    items(state.history, key = { it.itemId }) { entry -> ProgressCard(entry = entry, onPlay = onPlay) }
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

@Composable
private fun ProgressCard(entry: WatchHistory, onPlay: (itemId: Int, videoId: Int) -> Unit) {
    TvProgressCard(
        title = entry.title,
        meta = progressMeta(entry.progress),
        // Карточка 16:9 — берём кадр, а не вертикальный постер: тот обрезался бы по центру.
        posterUrl = entry.wideOrPoster,
        progress = entry.progress?.fraction ?: 0f,
        // Карточка с прогрессом ведёт в плеер: «продолжить» — это про воспроизведение, а не про
        // чтение описания. Эпизод берём из истории, позицию внутри трека восстановит плеер.
        onClick = { onPlay(entry.itemId, entry.progress?.videoId ?: NO_VIDEO_ID) },
        posterContent = { url, posterModifier ->
            TvPoster(url, entry.title, posterModifier, TvMetrics.CardShape)
        },
    )
}

/** Плитка папки-закладки. Фокусируется и открывается — содержимое грузит [LibraryScreenModel]. */
@Composable
private fun FolderTile(folder: BookmarkFolder, onClick: () -> Unit) {
    val count = folder.count
    val word = when {
        count % 100 in 11..14 -> "тайтлов"
        count % 10 == 1 -> "тайтл"
        count % 10 in 2..4 -> "тайтла"
        else -> "тайтлов"
    }
    TvFocusCard(
        onClick = onClick,
        shape = TvMetrics.PanelShape,
        modifier = Modifier.height(FolderTileHeight),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(TvMetrics.PanelShape)
                .background(TvSurfaceContainer)
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                folder.title,
                style = MaterialTheme.typography.titleMedium,
                color = TvOnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "$count $word",
                style = MaterialTheme.typography.bodySmall,
                color = TvOnSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

/** Постер для слота карточек дизайн-системы: монохромный плейсхолдер вместо розового по умолчанию. */
@Composable
private fun TvPoster(url: String, title: String, modifier: Modifier, shape: Shape) {
    PosterImage(
        url = url,
        contentDescription = title,
        modifier = modifier,
        shape = shape,
        accentColor = TvSurfaceContainerHighest,
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
        CircularProgressIndicator(color = TvAccent)
    }
}

/** Колонки сетки при ширине макета 960dp: постеры 190dp — вчетверо, карточки 16:9 250dp — втрое. */
private fun columnsFor(segment: MineSegment, folderOpen: Boolean): Int = when (segment) {
    MineSegment.CONTINUE, MineSegment.HISTORY -> WIDE_COLUMNS
    MineSegment.WATCHLIST -> POSTER_COLUMNS
    MineSegment.BOOKMARKS -> if (folderOpen) POSTER_COLUMNS else FOLDER_COLUMNS
}

/** Мета карточки 16:9: сезон и остаток — то, ради чего на неё вообще смотрят. */
private fun progressMeta(progress: WatchProgress?): String? {
    if (progress == null) return null
    val remaining = (progress.durationSeconds ?: 0) - (progress.timeSeconds ?: 0)
    val parts = buildList {
        progress.season?.let { season -> add("Сезон $season") }
        when {
            remaining >= SECONDS_IN_MINUTE -> add("Осталось ${remaining / SECONDS_IN_MINUTE} мин")
            progress.fraction > 0f -> add("Просмотрено")
        }
    }
    return parts.joinToString(" · ").ifBlank { null }
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

private val FolderTileHeight = 130.dp

private const val POSTER_COLUMNS = 4
private const val WIDE_COLUMNS = 3
private const val FOLDER_COLUMNS = 3

/** Фильм — единственный трек, эпизод выбирать не из чего: PlayerRoute.videoId = -1. */
private const val NO_VIDEO_ID = -1

/** Доля просмотра, при которой тайтл считается начатым, но не досмотренным. */
private const val CONTINUE_MIN_FRACTION = 0.01f
private const val CONTINUE_MAX_FRACTION = 0.95f

private const val SECONDS_IN_MINUTE = 60

/** За сколько карточек до конца сетки просить следующую страницу папки (примерно ряд). */
private const val LOAD_MORE_TAIL = 4

/** Сколько кадров подряд пробовать вернуть фокус в сетку после смены её содержимого. */
private const val FOCUS_ATTEMPTS = 3

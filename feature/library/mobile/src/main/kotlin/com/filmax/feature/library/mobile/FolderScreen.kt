// Открытая папка: шапка её цвета — плитка-иконка, название, счётчик и действия, ниже — сетка
// тайтлов с догрузкой страниц. Переименование и удаление — шторками, решает FolderScreenModel.

package com.filmax.feature.library.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.glassSource
import com.filmax.core.designsystem.glow
import com.filmax.core.designsystem.oklch
import com.filmax.core.designsystem.radialWash
import com.filmax.core.designsystem.rememberGlassState
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.presentation.folders.FolderInput
import com.filmax.core.ui.components.FilmaxButton
import com.filmax.core.ui.components.FilmaxButtonSize
import com.filmax.core.ui.components.FilmaxConfirmSheet
import com.filmax.core.ui.components.FilmaxEmptyState
import com.filmax.core.ui.components.FilmaxFolderEditor
import com.filmax.core.ui.components.FilmaxIconButton
import com.filmax.core.ui.components.FilmaxPosterCard
import com.filmax.core.ui.components.FilmaxPosterSkeleton
import com.filmax.core.ui.components.FilmaxSheet
import com.filmax.core.ui.components.FilmaxStatusBarGlass
import com.filmax.core.ui.components.FolderEditorActions
import com.filmax.core.ui.components.folderHue
import com.filmax.core.presentation.labels.posterMeta
import com.filmax.core.presentation.labels.ratingLabel
import com.filmax.core.presentation.labels.titlesLabel
import com.filmax.core.presentation.labels.typeLabel
import com.filmax.feature.library.common.FolderEvent
import com.filmax.feature.library.common.FolderScreenModel
import com.filmax.feature.library.common.FolderSideEffect
import com.filmax.feature.library.common.FolderState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** За сколько хвостовых ячеек до конца просить следующую страницу папки. */
private const val LOAD_MORE_TAIL = 6
private const val SKELETON_COUNT = 6

@Composable
fun FolderScreen(
    modifier: Modifier = Modifier,
    screenModel: FolderScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val state by screenModel.collectAsState()
    screenModel.collectSideEffect { effect ->
        when (effect) {
            FolderSideEffect.Deleted -> navigator.back()
        }
    }
    val hue = folderHue(state.folder?.tint ?: 0)
    Box(
        modifier
            .fillMaxSize()
            .background(FilmaxTheme.palette.background)
            .radialWash(
                oklch(lightness = 0.55f, chroma = 0.2f, hue = hue, alpha = 0.55f),
                centerX = 0.3f,
                centerY = 0f,
                radiusX = 1f,
                radiusY = 0.4f,
            ),
    ) {
        FolderGrid(
            state = state,
            hue = hue,
            onEvent = screenModel::dispatch,
            onOpenItem = { navigator.open(Destination.Details(it)) },
            onPlay = { navigator.open(Destination.Player(itemId = it)) },
        )
        FolderTopBar(onBack = navigator::back, onDelete = { screenModel.dispatch(FolderEvent.AskDelete) })
    }
    FolderSheets(state = state, onEvent = screenModel::dispatch)
}

@Composable
private fun FolderGrid(
    state: FolderState,
    hue: Float,
    onEvent: (FolderEvent) -> Unit,
    onOpenItem: (Int) -> Unit,
    onPlay: (Int) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val loadMore by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - LOAD_MORE_TAIL
        }
    }
    LaunchedEffect(loadMore) { if (loadMore) onEvent(FolderEvent.LoadMore) }
    val glass = rememberGlassState()
    val scrolled by remember {
        derivedStateOf { gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0 }
    }
    // Стекло под статус-баром — поверх сетки; кнопки шапки экран кладёт ещё выше.
    FilmaxStatusBarGlass(glass = glass, visible = scrolled, modifier = Modifier.zIndex(1f))
    LazyVerticalGrid(
        columns = GridCells.Fixed(FilmaxMetrics.POSTER_COLUMNS),
        state = gridState,
        modifier = Modifier.fillMaxSize().glassSource(glass),
        contentPadding = PaddingValues(
            start = FilmaxMetrics.GridPadding,
            end = FilmaxMetrics.GridPadding,
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(FilmaxMetrics.CardGap),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
            FolderHeader(
                state = state,
                hue = hue,
                onPlay = state.items.firstOrNull()?.let { first -> { onPlay(first.id) } },
                onRename = { onEvent(FolderEvent.StartRename) },
            )
        }
        folderItems(state = state, onEvent = onEvent, onOpenItem = onOpenItem)
    }
}

/** Тайтлы папки: скелетон первой загрузки, пустая папка или постеры с догрузкой по хвосту. */
private fun LazyGridScope.folderItems(state: FolderState, onEvent: (FolderEvent) -> Unit, onOpenItem: (Int) -> Unit) {
    when {
        state.loading && state.items.isEmpty() -> items(
            SKELETON_COUNT,
            key = { "skeleton-$it" }
        ) { FilmaxPosterSkeleton() }
        state.items.isEmpty() -> item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
            FilmaxEmptyState(
                Icons.Outlined.FolderOpen,
                "Папка пуста",
                subtitle = state.error ?: "Добавляйте тайтлы кнопкой «В папку» в их карточке",
                modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
            )
        }
    }
    items(state.items, key = { it.id }) { item ->
        FilmaxPosterCard(
            title = item.title,
            posterUrl = item.posters.medium,
            onClick = { onOpenItem(item.id) },
            rating = ratingLabel(item.rating.kinopoisk),
            meta = posterMeta(typeLabel(item.type), item.year),
            // Долгий тап убирает тайтл из папки — так же, как долгое «ОК» на пульте.
            onLongClick = { onEvent(FolderEvent.RemoveItem(item.id)) },
        )
    }
    if (state.loadingMore) {
        items(FilmaxMetrics.POSTER_COLUMNS, key = { "more-$it" }) { FilmaxPosterSkeleton() }
    }
}

/** Шапка папки: плитка её цвета со свечением, название, счётчик и действия. */
@Composable
private fun FolderHeader(state: FolderState, hue: Float, onPlay: (() -> Unit)?, onRename: () -> Unit) {
    val tile = oklch(lightness = 0.62f, chroma = 0.2f, hue = hue)
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = FolderTop, start = 4.dp, bottom = 8.dp)) {
        Box(
            Modifier
                .size(64.dp)
                .glow(tile.copy(alpha = TILE_GLOW_ALPHA), TileShape, blur = 30.dp, spread = 0.dp, offsetY = 8.dp)
                .clip(TileShape)
                .background(tile),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Folder, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
        }
        Text(
            state.folder?.title.orEmpty(),
            style = FilmaxType.headline,
            color = FilmaxColors.Text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 16.dp),
        )
        state.folder?.let {
            Text(
                titlesLabel(it.count),
                style = FilmaxType.bodySmall,
                color = FilmaxColors.TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Row(
            Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            onPlay?.let {
                FilmaxButton("Смотреть", onClick = it, icon = Icons.Filled.PlayArrow, size = FilmaxButtonSize.Compact)
            }
            FilmaxIconButton(Icons.Filled.Edit, contentDescription = "Переименовать", onClick = onRename)
        }
    }
}

@Composable
private fun FolderTopBar(onBack: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        FilmaxIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", onClick = onBack)
        FilmaxIconButton(Icons.Filled.Delete, contentDescription = "Удалить папку", onClick = onDelete)
    }
}

@Composable
private fun FolderSheets(state: FolderState, onEvent: (FolderEvent) -> Unit) {
    state.draft?.let { draft ->
        val actions = remember(onEvent) {
            FolderEditorActions(
                onTitleChange = { onEvent(FolderEvent.EditFolder(FolderInput.Text(it))) },
                onTintChange = { onEvent(FolderEvent.EditFolder(FolderInput.Tint(it))) },
                onSubmit = { onEvent(FolderEvent.SubmitRename) },
            )
        }
        FilmaxSheet(onDismiss = {
            onEvent(FolderEvent.CancelRename)
        }, title = "Переименовать", subtitle = "Название и цвет — на этом устройстве") {
            FilmaxFolderEditor(
                title = draft.title,
                tint = draft.tint,
                submitLabel = "Сохранить",
                actions = actions,
                submitIcon = Icons.Filled.Edit,
                canSubmit = draft.canSubmit,
            )
        }
    }
    if (state.confirmDelete) {
        FilmaxConfirmSheet(
            title = "Удалить папку?",
            message = "Папка «${state.folder?.title.orEmpty()}» удалится с аккаунта kino.pub. " +
                "Тайтлы в ней останутся в каталоге.",
            confirmLabel = "Удалить",
            onConfirm = { onEvent(FolderEvent.ConfirmDelete) },
            onDismiss = { onEvent(FolderEvent.DismissDelete) },
        )
    }
}

private val FolderTop = 56.dp
private val TileShape = RoundedCornerShape(22.dp)
private const val TILE_GLOW_ALPHA = 0.6f

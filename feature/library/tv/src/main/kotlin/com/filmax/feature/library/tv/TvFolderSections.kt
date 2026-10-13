// Секции экрана папки: шапка (иконка, название, действия) и сетка постеров с догрузкой.
package com.filmax.feature.library.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.user.model.UserFolder
import com.filmax.core.tv.designsystem.FxType
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.TvBackHint
import com.filmax.core.tv.designsystem.TvButton
import com.filmax.core.tv.designsystem.TvIconButton
import com.filmax.core.tv.designsystem.TvKeyHint
import com.filmax.core.tv.designsystem.TvKeyHints
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvPosterCard
import com.filmax.core.tv.designsystem.TvScreenFocus
import com.filmax.core.ui.components.durationLabel
import com.filmax.core.ui.components.isHighRating
import com.filmax.core.ui.components.posterMeta
import com.filmax.core.ui.components.ratingLabel
import com.filmax.core.ui.components.titlesLabel
import com.filmax.feature.library.common.FolderEvent
import com.filmax.feature.library.common.FolderState

/** Ключ «Смотреть подряд» — цель фокуса, когда в папке нет ни одного постера. */
internal const val PLAY_ALL_KEY = "folder:playAll"

private val IconTileSize = 64.dp
private val IconTileShape = RoundedCornerShape(18.dp)
private const val ICON_TILE_ALPHA = 0.18f
private const val SUBTITLE_ALPHA = 0.75f
private val FolderTitle = FxType.screenTitle.copy(
    fontWeight = FontWeight.ExtraBold,
    fontSize = 38.sp,
    lineHeight = 40.sp
)

/** Сетка папки: шесть постеров 132dp в ряд, правое поле уже — так в ряд входит ровно шесть. */
private const val GRID_COLUMNS = 6
private val GridGap = 16.dp
private val GridEnd = 30.dp

/** За сколько постеров до конца просить следующую страницу — примерно ряд. */
private const val LOAD_MORE_TAIL = 6

/** Фокус шапки: куда вернуться после панели и диалога, и память экрана. */
internal data class FolderHeaderFocus(val targets: FolderFocus, val screen: TvScreenFocus)

@Composable
internal fun FolderHeader(folder: UserFolder, state: FolderState, actions: FolderActions, focus: FolderHeaderFocus) {
    Column(Modifier.padding(start = TvMetrics.SafeHorizontal, end = TvMetrics.SectionEnd, top = 24.dp)) {
        TvBackHint("Моё · Папки")
        Row(
            Modifier.fillMaxWidth().padding(top = 22.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            FolderTitleBlock(folder, Modifier.weight(1f))
            FolderButtons(state.items.firstOrNull(), actions, focus)
        }
    }
}

@Composable
private fun FolderTitleBlock(folder: UserFolder, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Box(
            Modifier.size(IconTileSize).background(Color.White.copy(alpha = ICON_TILE_ALPHA), IconTileShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Folder, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
        }
        Column {
            Text(folder.title, style = FolderTitle, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${titlesLabel(folder.count)} · ваша папка",
                style = FxType.body,
                color = Color.White.copy(alpha = SUBTITLE_ALPHA),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** «Смотреть подряд» (с первого тайтла), «Переименовать», «Удалить». */
@Composable
private fun FolderButtons(first: Item?, actions: FolderActions, focus: FolderHeaderFocus) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (first != null) {
            TvButton(
                text = "Смотреть подряд",
                onClick = { actions.onPlay(first.id) },
                leadingIcon = Icons.Rounded.PlayArrow,
                modifier = focus.screen.item(PLAY_ALL_KEY),
            )
        }
        TvIconButton(
            icon = Icons.Rounded.Edit,
            contentDescription = "Переименовать",
            onClick = { actions.onEvent(FolderEvent.StartRename) },
            modifier = Modifier.focusRequester(focus.targets.rename),
        )
        TvIconButton(
            icon = Icons.Rounded.Delete,
            contentDescription = "Удалить папку",
            onClick = { actions.onEvent(FolderEvent.AskDelete) },
            modifier = Modifier.focusRequester(focus.targets.delete),
        )
    }
}

@Composable
internal fun FolderGrid(state: FolderState, actions: FolderActions, screenFocus: TvScreenFocus) {
    val gridState = rememberLazyGridState()
    val handoff = rememberCardFocusHandoff()
    LoadMoreWhenNearEnd(gridState) { actions.onEvent(FolderEvent.LoadMore) }
    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(GRID_COLUMNS),
            state = gridState,
            modifier = Modifier.fillMaxSize().then(handoff.gridModifier),
            horizontalArrangement = Arrangement.spacedBy(GridGap),
            verticalArrangement = Arrangement.spacedBy(TvMetrics.CardGap),
            contentPadding = PaddingValues(
                start = TvMetrics.SafeHorizontal,
                end = GridEnd,
                top = 28.dp,
                bottom = HINTS_SPACE,
            ),
        ) {
            if (state.items.isEmpty()) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) { EmptyFolder(actions) }
            }
            items(state.items, key = { it.id }) { item ->
                FolderPoster(item, state.items, actions, screenFocus, handoff)
            }
        }
        FolderHints(Modifier.align(Alignment.BottomStart))
    }
}

@Composable
private fun FolderPoster(
    item: Item,
    all: List<Item>,
    actions: FolderActions,
    screenFocus: TvScreenFocus,
    handoff: CardFocusHandoff,
) {
    val key = folderItemKey(item.id)
    TvPosterCard(
        title = item.title,
        meta = posterMeta(type = null, year = item.year).withDuration(item),
        posterUrl = item.posters.medium.ifBlank { item.posters.small },
        onClick = { actions.onOpenItem(item.id) },
        // Долгое «ОК» — убрать из папки; фокус переходит на соседа, а не теряется.
        onLongClick = {
            handoff.handOff(all.map { folderItemKey(it.id) }, key)
            actions.onEvent(FolderEvent.RemoveItem(item.id))
        },
        rating = ratingLabel(item.rating.kinopoisk),
        ratingHighlighted = isHighRating(item.rating.kinopoisk),
        modifier = screenFocus.item(key).then(handoff.card(key)),
    )
}

/** «2021 · 1 ч 52 мин» — у фильма длительность, у сериала только год. */
private fun String?.withDuration(item: Item): String? {
    val minutes = item.duration.averageMinutes?.toInt()?.takeIf { it > 0 && item.type == ItemType.MOVIE }
    return listOfNotNull(this, minutes?.let(::durationLabel)).joinToString(" · ").ifBlank { null }
}

/** Пустая папка: что здесь будет и «Перейти в каталог» — единственное осмысленное действие. */
@Composable
private fun EmptyFolder(actions: FolderActions) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        MineEmpty(MineEmptyText.Folder, Icons.Rounded.FolderOpen)
        TvButton(text = "Перейти в каталог", onClick = actions.onOpenCatalog, primary = false)
    }
}

/** Подсказки пульта внизу: долгое «ОК» не видно, о нём надо сказать. */
@Composable
private fun FolderHints(modifier: Modifier) {
    val ok = Icons.Rounded.RadioButtonChecked
    TvKeyHints(
        hints = listOf(
            TvKeyHint(listOf(ok), "открыть"),
            TvKeyHint(listOf(ok), "удерживать — убрать из папки"),
            TvKeyHint(listOf(Icons.Rounded.KeyboardArrowUp), "действия с папкой"),
        ),
        modifier = modifier
            .fillMaxWidth()
            .background(LocalFxColors.current.background)
            .padding(start = TvMetrics.SafeHorizontal, top = 10.dp, bottom = 22.dp),
    )
}

private val HINTS_SPACE = 64.dp

/** Сетка подошла к концу — догрузить следующую страницу папки. */
@Composable
private fun LoadMoreWhenNearEnd(gridState: LazyGridState, onLoadMore: () -> Unit) {
    val nearEnd by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - LOAD_MORE_TAIL
        }
    }
    LaunchedEffect(nearEnd) { if (nearEnd) onLoadMore() }
}

internal fun folderItemKey(itemId: Int) = "folderItem:$itemId"

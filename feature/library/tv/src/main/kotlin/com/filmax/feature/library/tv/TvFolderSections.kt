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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.user.model.UserFolder
import com.filmax.core.presentation.labels.durationLabel
import com.filmax.core.presentation.labels.isHighRating
import com.filmax.core.presentation.labels.posterMeta
import com.filmax.core.presentation.labels.ratingLabel
import com.filmax.core.presentation.labels.titlesLabel
import com.filmax.core.tv.designsystem.FxType
import com.filmax.core.tv.designsystem.TvBackHint
import com.filmax.core.tv.designsystem.TvButton
import com.filmax.core.tv.designsystem.TvButtonStyle
import com.filmax.core.tv.designsystem.TvDimGroup
import com.filmax.core.tv.designsystem.TvIconButton
import com.filmax.core.tv.designsystem.TvKeyHint
import com.filmax.core.tv.designsystem.TvKeyHints
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvPosterCard
import com.filmax.core.tv.designsystem.TvScreenFocus
import com.filmax.core.tv.designsystem.fxEnter
import com.filmax.core.tv.designsystem.rememberTvScrollFades
import com.filmax.core.tv.designsystem.tvScrollFades
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
    lineHeight = 38.sp,
    letterSpacing = (-0.03).em,
)

/**
 * Шапка занимает верх экрана до сетки: «назад» на 24dp, название — на 70dp, верх постеров — на
 * 200dp. Высота фиксирована, чтобы сетка стояла на месте при любом названии.
 */
private val HeaderHeight = 180.dp
private val HeaderTop = 24.dp
private val TitleRowTop = 24.dp
private val GridTop = 200.dp - HeaderHeight

/** Сетка папки: шесть постеров 132dp в ряд, правое поле уже — так в ряд входит ровно шесть. */
private const val GRID_COLUMNS = 6
private val GridGap = 16.dp
private val GridEnd = 30.dp

/** Низ сетки гаснет под подсказками пульта — без отдельной плашки под ними. */
private val GridFade = 60.dp
private val HintsSpace = 64.dp
private val HintsBottom = 22.dp

/** Шаги появления: иконка и название, подпись, кнопки; постеры — вместе с кнопками и дальше. */
private const val SUBTITLE_ENTER = 1
private const val BUTTONS_ENTER = 2
private const val FIRST_POSTER_ENTER = 2

/** За сколько постеров до конца просить следующую страницу — примерно ряд. */
private const val LOAD_MORE_TAIL = 6

/** Надпись на колпачке «ОК» — так клавиша подписана на пульте. */
private const val OK_KEY = "OK"

/** Фокус шапки: куда вернуться после панели и диалога, и память экрана. */
internal data class FolderHeaderFocus(val targets: FolderFocus, val screen: TvScreenFocus)

@Composable
internal fun FolderHeader(
    folder: UserFolder,
    state: FolderState,
    onEvent: (FolderEvent) -> Unit,
    focus: FolderHeaderFocus,
) {
    Column(
        Modifier
            .height(HeaderHeight)
            .padding(start = TvMetrics.SafeHorizontal, end = TvMetrics.SectionEnd, top = HeaderTop),
    ) {
        TvBackHint("Моё · Папки")
        Row(
            Modifier.fillMaxWidth().padding(top = TitleRowTop),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            FolderTitleBlock(folder, Modifier.weight(1f))
            FolderButtons(hasItems = state.items.isNotEmpty(), onEvent, focus, Modifier.fxEnter(BUTTONS_ENTER))
        }
    }
}

@Composable
private fun FolderTitleBlock(folder: UserFolder, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Box(
            Modifier
                .fxEnter(0)
                .size(IconTileSize)
                .background(Color.White.copy(alpha = ICON_TILE_ALPHA), IconTileShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Folder, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
        }
        Column {
            Text(
                folder.title,
                style = FolderTitle,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fxEnter(0),
            )
            Text(
                "${titlesLabel(folder.count)} · ваша папка",
                style = FxType.metaRow,
                color = Color.White.copy(alpha = SUBTITLE_ALPHA),
                modifier = Modifier.padding(top = 8.dp).fxEnter(SUBTITLE_ENTER),
            )
        }
    }
}

/**
 * «Смотреть подряд» (с первого тайтла) — белая: на цветной шапке акцентная заливка спорила бы
 * с цветом папки. «Переименовать» и «Удалить» — круглые, удаление — красным.
 */
@Composable
private fun FolderButtons(
    hasItems: Boolean,
    onEvent: (FolderEvent) -> Unit,
    focus: FolderHeaderFocus,
    modifier: Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (hasItems) {
            TvButton(
                text = "Смотреть подряд",
                onClick = { onEvent(FolderEvent.PlayAll) },
                style = TvButtonStyle.Light,
                leadingIcon = Icons.Rounded.PlayArrow,
                modifier = focus.screen.item(PLAY_ALL_KEY),
            )
        }
        TvIconButton(
            icon = Icons.Rounded.Edit,
            contentDescription = "Переименовать",
            onClick = { onEvent(FolderEvent.StartRename) },
            modifier = Modifier.focusRequester(focus.targets.rename),
        )
        TvIconButton(
            icon = Icons.Rounded.Delete,
            contentDescription = "Удалить папку",
            onClick = { onEvent(FolderEvent.AskDelete) },
            modifier = Modifier.focusRequester(focus.targets.delete),
            destructive = true,
        )
    }
}

/**
 * Сетка постеров папки. Пока фокус в ней, соседи постера в фокусе приглушены; низ гаснет под
 * подсказками пульта.
 */
@Composable
internal fun FolderGrid(state: FolderState, onEvent: (FolderEvent) -> Unit, screenFocus: TvScreenFocus) {
    val gridState = rememberLazyGridState()
    val cards = FolderCards(screenFocus, rememberCardFocusHandoff(), state.items.map { folderItemKey(it.id) })
    val fades = rememberTvScrollFades(gridState)
    LoadMoreWhenNearEnd(gridState) { onEvent(FolderEvent.LoadMore) }
    Box(Modifier.fillMaxSize()) {
        TvDimGroup(Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(GRID_COLUMNS),
                state = gridState,
                modifier = Modifier.fillMaxSize().tvScrollFades(fades, GridFade).then(cards.handoff.gridModifier),
                horizontalArrangement = Arrangement.spacedBy(GridGap),
                verticalArrangement = Arrangement.spacedBy(TvMetrics.CardGap),
                contentPadding = PaddingValues(
                    start = TvMetrics.SafeHorizontal,
                    end = GridEnd,
                    top = GridTop,
                    bottom = HintsSpace,
                ),
            ) {
                if (state.items.isEmpty()) {
                    item(key = "empty", span = { GridItemSpan(maxLineSpan) }) { EmptyFolder(onEvent) }
                }
                itemsIndexed(state.items, key = { _, item -> item.id }) { index, item ->
                    FolderPoster(item, index, onEvent, cards)
                }
            }
        }
        FolderHints(Modifier.align(Alignment.BottomStart))
    }
}

/** Фокус постеров: память экрана, передача соседу при удалении и порядок ключей для неё. */
private class FolderCards(val screen: TvScreenFocus, val handoff: CardFocusHandoff, val keys: List<String>)

@Composable
private fun LazyGridItemScope.FolderPoster(item: Item, index: Int, onEvent: (FolderEvent) -> Unit, cards: FolderCards) {
    val key = folderItemKey(item.id)
    var removing by remember { mutableStateOf(false) }
    TvPosterCard(
        title = item.title,
        meta = posterMeta(type = null, year = item.year).withDuration(item),
        posterUrl = item.posters.medium.ifBlank { item.posters.small },
        onClick = { onEvent(FolderEvent.OpenItem(item.id)) },
        // Долгое «ОК» — убрать из папки; фокус переходит на соседа, а не теряется.
        onLongClick = {
            cards.handoff.handOff(cards.keys, key)
            removing = true
        },
        rating = ratingLabel(item.rating.kinopoisk),
        ratingHighlighted = isHighRating(item.rating.kinopoisk),
        modifier = Modifier
            .animateItem(fadeOutSpec = null)
            .fxEnter(FIRST_POSTER_ENTER + index % GRID_COLUMNS)
            .then(cards.screen.item(key))
            .then(cards.handoff.card(key))
            .collapseOnRemove(removing) { onEvent(FolderEvent.RemoveItem(item.id)) },
    )
}

/** «2021 · 1 ч 52 мин» — у фильма длительность, у сериала только год. */
private fun String?.withDuration(item: Item): String? {
    val minutes = item.duration.averageMinutes?.toInt()?.takeIf { it > 0 && item.type == ItemType.MOVIE }
    return listOfNotNull(this, minutes?.let(::durationLabel)).joinToString(" · ").ifBlank { null }
}

/** Пустая папка: что здесь будет и «Перейти в каталог» — единственное осмысленное действие. */
@Composable
private fun EmptyFolder(onEvent: (FolderEvent) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        MineEmpty(MineEmptyText.Folder, Icons.Rounded.FolderOpen)
        TvButton(
            text = "Перейти в каталог",
            onClick = { onEvent(FolderEvent.OpenCatalog) },
            style = TvButtonStyle.Secondary,
        )
    }
}

/** Подсказки пульта внизу: долгое «ОК» не видно, о нём надо сказать. */
@Composable
private fun FolderHints(modifier: Modifier) {
    TvKeyHints(
        hints = listOf(
            TvKeyHint(keys = emptyList(), label = "открыть", keyText = OK_KEY),
            TvKeyHint(keys = emptyList(), label = "удерж. убрать из папки", keyText = OK_KEY),
            TvKeyHint(listOf(Icons.Rounded.KeyboardArrowUp), "действия с папкой"),
        ),
        modifier = modifier.padding(start = TvMetrics.SafeHorizontal, bottom = HintsBottom),
    )
}

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

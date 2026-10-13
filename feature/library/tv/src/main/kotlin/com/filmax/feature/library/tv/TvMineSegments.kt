// Содержимое сегментов «Моё»: сетки «Продолжить», «Буду смотреть», «Папки» и «История».
// Смена сегмента — кроссфейдом 200 мс; карточки — из дизайн-системы, ключи фокуса — «сегмент:id».
package com.filmax.feature.library.tv

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.filmax.core.domain.favorites.model.FavoriteItem
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.tv.designsystem.FxMotion
import com.filmax.core.tv.designsystem.ScrollToTopOnNavFocus
import com.filmax.core.tv.designsystem.TvCardSize
import com.filmax.core.tv.designsystem.TvCreateFolderCard
import com.filmax.core.tv.designsystem.TvFolderCard
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOverline
import com.filmax.core.tv.designsystem.TvPosterCard
import com.filmax.core.tv.designsystem.TvPosterSize
import com.filmax.core.tv.designsystem.TvProgressCard
import com.filmax.core.tv.designsystem.TvScreenFocus
import com.filmax.core.tv.designsystem.tvFolderTint
import com.filmax.core.ui.components.isHighRating
import com.filmax.core.ui.components.posterMeta
import com.filmax.core.ui.components.ratingLabel
import com.filmax.feature.library.common.HistoryDay
import com.filmax.feature.library.common.MineEvent
import com.filmax.feature.library.common.MineSegment
import com.filmax.feature.library.common.MineState

/** Раскладка сетки сегмента: колонки и зазоры из макета (ширина контента 840dp). */
private data class GridSpec(val columns: Int, val columnGap: Dp, val rowGap: Dp)

private val ContinueGrid = GridSpec(columns = 3, columnGap = 24.dp, rowGap = 22.dp)
private val WatchlistGrid = GridSpec(columns = 5, columnGap = 20.dp, rowGap = 24.dp)
private val FoldersGrid = GridSpec(columns = 3, columnGap = 24.dp, rowGap = 26.dp)
private val HistoryGrid = GridSpec(columns = 4, columnGap = 20.dp, rowGap = 18.dp)

/** Смена сегмента — кроссфейд; длительность — токен «fade» (200 мс). */
private const val SEGMENT_FADE_MS = 200

@Composable
internal fun MineSegmentContent(state: MineState, actions: MineActions, focus: MineFocus, screenFocus: TvScreenFocus) {
    AnimatedContent(
        targetState = state.segment,
        transitionSpec = { fadeIn(tween(SEGMENT_FADE_MS)) togetherWith fadeOut(FxMotion.fade()) },
        label = "mineSegment",
    ) { segment ->
        val grid = gridSpec(segment)
        val cards = GridFocus(screenFocus, rememberCardFocusHandoff())
        MineGrid(grid, cards) {
            when (segment) {
                MineSegment.CONTINUE -> continueItems(state.continueWatching, actions, cards)
                MineSegment.WATCHLIST -> watchlistItems(state.watchlist, actions, cards)
                MineSegment.FOLDERS -> folderItems(state, actions, focus, screenFocus)
                MineSegment.HISTORY -> historyItems(state.history, actions, screenFocus)
            }
        }
    }
}

private fun gridSpec(segment: MineSegment): GridSpec = when (segment) {
    MineSegment.CONTINUE -> ContinueGrid
    MineSegment.WATCHLIST -> WatchlistGrid
    MineSegment.FOLDERS -> FoldersGrid
    MineSegment.HISTORY -> HistoryGrid
}

/**
 * Сетка сегмента. Поля — в contentPadding, а не на родителе: сетка не клипает свои поля, и рамка
 * со свечением крайних карточек не срезается.
 */
@Composable
private fun MineGrid(spec: GridSpec, cards: GridFocus, content: LazyGridScope.() -> Unit) {
    val gridState = rememberLazyGridState()
    ScrollToTopOnNavFocus(gridState)
    LazyVerticalGrid(
        columns = GridCells.Fixed(spec.columns),
        state = gridState,
        modifier = Modifier.fillMaxSize().then(cards.handoff.gridModifier),
        horizontalArrangement = Arrangement.spacedBy(spec.columnGap),
        verticalArrangement = Arrangement.spacedBy(spec.rowGap),
        contentPadding = PaddingValues(
            start = TvMetrics.SectionStart,
            end = TvMetrics.SectionEnd,
            top = TvMetrics.FocusInset + 8.dp,
            bottom = TvMetrics.SafeVertical + TvMetrics.FocusInset,
        ),
        content = content,
    )
}

/** Фокус карточек сетки: память экрана и передача фокуса соседу при удалении. */
private class GridFocus(val screen: TvScreenFocus, val handoff: CardFocusHandoff)

private fun LazyGridScope.continueItems(entries: List<WatchHistory>, actions: MineActions, cards: GridFocus) {
    if (entries.isEmpty()) {
        emptyItem(MineEmptyText.Continue, Icons.Rounded.PlayCircleOutline)
        return
    }
    val keys = entries.map { continueKey(it.itemId) }
    items(entries, key = { it.itemId }) { entry ->
        val key = continueKey(entry.itemId)
        TvProgressCard(
            title = entry.title,
            meta = remainingLabel(entry.progress),
            posterUrl = entry.wideOrPoster,
            progress = entry.progress?.fraction ?: 0f,
            // OK — сразу в плеер, на ту же серию: номер серии уникален только внутри сезона.
            onClick = {
                actions.onPlay(entry.itemId, entry.progress?.season ?: NO_SEASON, entry.progress?.videoId ?: NO_VIDEO)
            },
            onLongClick = {
                cards.handoff.handOff(keys, key)
                actions.onEvent(MineEvent.RemoveFromContinue(entry.itemId))
            },
            size = TvCardSize.Grid,
            badge = episodeBadge(entry.progress),
            modifier = Modifier
                .animateItem(fadeOutSpec = tween(REMOVE_FADE_MS))
                .then(cards.screen.item(key))
                .then(cards.handoff.card(key)),
        )
    }
}

private fun LazyGridScope.watchlistItems(items: List<FavoriteItem>, actions: MineActions, cards: GridFocus) {
    if (items.isEmpty()) {
        emptyItem(MineEmptyText.Watchlist, Icons.Rounded.BookmarkBorder)
        return
    }
    val keys = items.map { watchlistKey(it.id) }
    items(items, key = { it.id }) { item ->
        val key = watchlistKey(item.id)
        TvPosterCard(
            title = item.title,
            meta = posterMeta(type = null, year = item.year),
            posterUrl = item.posterSmall,
            onClick = { actions.onOpenItem(item.id) },
            // Долгое «ОК» — убрать из списка; фокус переходит на соседа, а не теряется.
            onLongClick = {
                cards.handoff.handOff(keys, key)
                actions.onEvent(MineEvent.RemoveFromWatchlist(item.id))
            },
            rating = ratingLabel(item.rating),
            ratingHighlighted = isHighRating(item.rating),
            size = TvPosterSize.Grid,
            modifier = Modifier
                .animateItem(fadeOutSpec = tween(REMOVE_FADE_MS))
                .then(cards.screen.item(key))
                .then(cards.handoff.card(key)),
        )
    }
}

private fun LazyGridScope.folderItems(
    state: MineState,
    actions: MineActions,
    focus: MineFocus,
    screenFocus: TvScreenFocus,
) {
    item(key = CREATE_FOLDER_KEY) {
        TvCreateFolderCard(
            onClick = { actions.onEvent(MineEvent.StartCreateFolder) },
            modifier = screenFocus.item(CREATE_FOLDER_KEY).focusRequester(focus.createFolder),
        )
    }
    items(state.folders, key = { it.folder.id }) { folder ->
        TvFolderCard(
            title = folder.folder.title,
            caption = folderCaption(folder),
            posters = folder.posters,
            tint = tvFolderTint(folder.folder.tint).card,
            onClick = { actions.onOpenFolder(folder.folder.id) },
            modifier = screenFocus.item(folderKey(folder.folder.id)),
        )
    }
}

private fun LazyGridScope.historyItems(days: List<HistoryDay>, actions: MineActions, screenFocus: TvScreenFocus) {
    if (days.isEmpty()) {
        emptyItem(MineEmptyText.History, Icons.Rounded.History)
        return
    }
    days.forEach { day ->
        item(key = "day:${day.label}", span = { GridItemSpan(maxLineSpan) }) {
            TvOverline(day.label, accent = false)
        }
        items(day.entries, key = { it.entry.itemId }) { (entry, time) ->
            TvProgressCard(
                title = entry.title,
                meta = historyMeta(entry.progress, time),
                posterUrl = entry.wideOrPoster,
                progress = entry.progress?.fraction ?: 0f,
                onClick = { actions.onOpenItem(entry.itemId) },
                size = TvCardSize.Small,
                modifier = screenFocus.item(historyKey(entry.itemId)),
            )
        }
    }
}

private fun LazyGridScope.emptyItem(text: MineEmptyText, icon: ImageVector) {
    item(key = "empty", span = { GridItemSpan(maxLineSpan) }) { MineEmpty(text, icon) }
}

/** Карточка схлопывается за 200 мс — как в макете, соседи съезжают на её место. */
private const val REMOVE_FADE_MS = 200

/** Фильм или неизвестная серия: плеер возьмёт первый трек (PlayerRoute = -1). */
private const val NO_SEASON = -1
private const val NO_VIDEO = -1

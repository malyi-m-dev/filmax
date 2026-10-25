// Содержимое сегментов «Моё»: сетки «Продолжить», «Буду смотреть», «Папки» и ряды «Истории».
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
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.filmax.core.tv.designsystem.TvDimGroup
import com.filmax.core.tv.designsystem.TvFolderCard
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvPosterCard
import com.filmax.core.tv.designsystem.TvPosterSize
import com.filmax.core.tv.designsystem.TvProgressCard
import com.filmax.core.tv.designsystem.TvScreenFocus
import com.filmax.core.tv.designsystem.fxEnter
import com.filmax.core.tv.designsystem.rememberTvScrollFades
import com.filmax.core.tv.designsystem.tvFolderTint
import com.filmax.core.tv.designsystem.tvScrollFades
import com.filmax.core.presentation.labels.isHighRating
import com.filmax.core.presentation.labels.posterMeta
import com.filmax.core.presentation.labels.ratingLabel
import com.filmax.feature.library.common.MineEvent
import com.filmax.feature.library.common.MineSegment
import com.filmax.feature.library.common.MineState

/** Раскладка сетки сегмента: колонки и зазоры из макета (ширина контента 840dp). */
private data class GridSpec(val columns: Int, val columnGap: Dp, val rowGap: Dp)

private val ContinueGrid = GridSpec(columns = 3, columnGap = 24.dp, rowGap = 22.dp)
private val WatchlistGrid = GridSpec(columns = 5, columnGap = 20.dp, rowGap = 24.dp)
private val FoldersGrid = GridSpec(columns = 3, columnGap = 24.dp, rowGap = 26.dp)

/** Смена сегмента — кроссфейд; длительность — токен «fade» (200 мс). */
private const val SEGMENT_FADE_MS = 200

/** Строка в фокусе встаёт верхом на эту высоту от верха сетки — предыдущая видна над ней, как в каталоге. */
private val GridPivot = 110.dp

/** Низ сетки гаснет на 60dp: видно, что ниже есть ещё ряд. */
private val GridFade = 60.dp

/**
 * Карточки появляются по очереди после заголовка и сегментов (они — первые два шага); в строке —
 * слева направо, следующие строки, доехав прокруткой, — так же.
 */
internal const val FIRST_CARD_ENTER = 2

@Composable
internal fun MineSegmentContent(state: MineState, actions: MineActions, focus: MineFocus, screenFocus: TvScreenFocus) {
    AnimatedContent(
        targetState = state.segment,
        transitionSpec = { fadeIn(tween(SEGMENT_FADE_MS)) togetherWith fadeOut(FxMotion.fade()) },
        label = "mineSegment",
    ) { segment ->
        val cards = GridFocus(screenFocus, rememberCardFocusHandoff())
        when (segment) {
            MineSegment.CONTINUE -> MineGrid(ContinueGrid, cards) {
                continueItems(state.continueWatching, actions, cards)
            }
            MineSegment.WATCHLIST -> MineGrid(WatchlistGrid, cards) { watchlistItems(state.watchlist, actions, cards) }
            MineSegment.FOLDERS -> MineGrid(FoldersGrid, cards) { folderItems(state, actions, focus, screenFocus) }
            MineSegment.HISTORY -> MineHistory(state.history, actions, screenFocus)
        }
    }
}

/**
 * Сетка сегмента. Поля — в contentPadding, а не на родителе: сетка не клипает свои поля, и рамка
 * со свечением крайних карточек не срезается. Пока фокус в сетке, соседи карточки в фокусе
 * приглушены; низ гаснет, пока ниже есть ряды.
 */
@Composable
private fun MineGrid(spec: GridSpec, cards: GridFocus, content: LazyGridScope.() -> Unit) {
    val gridState = rememberLazyGridState()
    ScrollToTopOnNavFocus(gridState)
    val fades = rememberTvScrollFades(gridState)
    PivotScroll(GridPivot) {
        TvDimGroup(Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(spec.columns),
                state = gridState,
                modifier = Modifier.fillMaxSize().tvScrollFades(fades, GridFade).then(cards.handoff.gridModifier),
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
    }
}

/** Фокус карточек сетки: память экрана и передача фокуса соседу при удалении. */
private class GridFocus(val screen: TvScreenFocus, val handoff: CardFocusHandoff)

private fun LazyGridScope.continueItems(entries: List<WatchHistory>, actions: MineActions, cards: GridFocus) {
    if (entries.isEmpty()) {
        emptyItem(MineEmptyText.Continue, Icons.Rounded.PlayCircleOutline)
        return
    }
    val keys = entries.map { continueKey(it.itemId) }
    itemsIndexed(entries, key = { _, entry -> entry.itemId }) { index, entry ->
        val key = continueKey(entry.itemId)
        var removing by remember { mutableStateOf(false) }
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
                removing = true
            },
            size = TvCardSize.Grid,
            badge = episodeBadge(entry.progress),
            modifier = removableCard(index % ContinueGrid.columns, key, cards)
                .collapseOnRemove(removing) { actions.onEvent(MineEvent.RemoveFromContinue(entry.itemId)) },
        )
    }
}

private fun LazyGridScope.watchlistItems(items: List<FavoriteItem>, actions: MineActions, cards: GridFocus) {
    if (items.isEmpty()) {
        emptyItem(MineEmptyText.Watchlist, Icons.Rounded.BookmarkBorder)
        return
    }
    val keys = items.map { watchlistKey(it.id) }
    itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
        val key = watchlistKey(item.id)
        var removing by remember { mutableStateOf(false) }
        TvPosterCard(
            title = item.title,
            meta = posterMeta(type = null, year = item.year),
            posterUrl = item.posterSmall,
            onClick = { actions.onOpenItem(item.id) },
            // Долгое «ОК» — убрать из списка; фокус переходит на соседа, а не теряется.
            onLongClick = {
                cards.handoff.handOff(keys, key)
                removing = true
            },
            rating = ratingLabel(item.rating),
            ratingHighlighted = isHighRating(item.rating),
            size = TvPosterSize.Grid,
            modifier = removableCard(index % WatchlistGrid.columns, key, cards)
                .collapseOnRemove(removing) { actions.onEvent(MineEvent.RemoveFromWatchlist(item.id)) },
        )
    }
}

/**
 * Карточка, которую можно убрать долгим «ОК»: появляется по очереди, соседи съезжают на место
 * убранной. Своего затухания у ухода нет — карточка уже схлопнулась ([collapseOnRemove]).
 */
@Composable
private fun LazyGridItemScope.removableCard(column: Int, key: String, cards: GridFocus): Modifier =
    Modifier
        .animateItem(fadeOutSpec = null)
        .fxEnter(FIRST_CARD_ENTER + column)
        .then(cards.screen.item(key))
        .then(cards.handoff.card(key))

private fun LazyGridScope.folderItems(
    state: MineState,
    actions: MineActions,
    focus: MineFocus,
    screenFocus: TvScreenFocus,
) {
    item(key = CREATE_FOLDER_KEY) {
        TvCreateFolderCard(
            onClick = { actions.onEvent(MineEvent.StartCreateFolder) },
            modifier = Modifier
                .fxEnter(FIRST_CARD_ENTER)
                .then(screenFocus.item(CREATE_FOLDER_KEY))
                .focusRequester(focus.createFolder),
        )
    }
    itemsIndexed(state.folders, key = { _, folder -> folder.folder.id }) { index, folder ->
        TvFolderCard(
            title = folder.folder.title,
            caption = folderCaption(folder),
            posters = folder.posters,
            tint = tvFolderTint(folder.folder.tint).card,
            onClick = { actions.onOpenFolder(folder.folder.id) },
            // Первая ячейка — «Создать папку»: папки идут в сетке со второй.
            modifier = Modifier
                .fxEnter(FIRST_CARD_ENTER + (index + 1) % FoldersGrid.columns)
                .then(screenFocus.item(folderKey(folder.folder.id))),
        )
    }
}

private fun LazyGridScope.emptyItem(text: MineEmptyText, icon: ImageVector) {
    item(key = "empty", span = { GridItemSpan(maxLineSpan) }) { MineEmpty(text, icon) }
}

/** Фильм или неизвестная серия: плеер возьмёт первый трек (PlayerRoute = -1). */
private const val NO_SEASON = -1
private const val NO_VIDEO = -1

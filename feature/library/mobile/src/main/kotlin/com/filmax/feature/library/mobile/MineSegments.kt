// Сегменты «Моё»: «Продолжить» кадрами, «Буду смотреть» постерами, папки веером и история по
// дням. Порядок, сортировки и удаление решает MineScreenModel; страницы лишь рисуют состояние.

package com.filmax.feature.library.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.ui.components.FilmaxChip
import com.filmax.core.ui.components.FilmaxContinueCard
import com.filmax.core.ui.components.FilmaxCreateFolderCard
import com.filmax.core.ui.components.FilmaxEmptyState
import com.filmax.core.ui.components.FilmaxFolderCard
import com.filmax.core.ui.components.FilmaxHistoryRow
import com.filmax.core.ui.components.FilmaxPosterCard
import com.filmax.core.ui.components.FilmaxTextButton
import com.filmax.core.ui.components.continueMeta
import com.filmax.core.ui.components.folderHue
import com.filmax.core.ui.components.ratingLabel
import com.filmax.core.ui.components.titlesLabel
import com.filmax.feature.library.common.MineEvent
import com.filmax.feature.library.common.MineSegment
import com.filmax.feature.library.common.MineState

/** Без сезона и серии: плеер возьмёт фильм или продолжит с сохранённого. */
private const val NO_EPISODE = -1

@Composable
internal fun MineSegmentPage(
    segment: MineSegment,
    state: MineState,
    actions: MineActions,
    contentPadding: PaddingValues
) {
    when (segment) {
        MineSegment.CONTINUE -> ContinuePage(state, actions, contentPadding)
        MineSegment.WATCHLIST -> WatchlistPage(state, actions, contentPadding)
        MineSegment.FOLDERS -> FoldersPage(state, actions, contentPadding)
        MineSegment.HISTORY -> HistoryPage(state, actions, contentPadding)
    }
}

@Composable
private fun ContinuePage(state: MineState, actions: MineActions, contentPadding: PaddingValues) {
    MineGrid(columns = FilmaxMetrics.WIDE_COLUMNS, contentPadding = contentPadding) {
        when {
            state.loading && state.continueWatching.isEmpty() -> loading()
            state.continueWatching.isEmpty() -> empty(
                Icons.Filled.PlayCircle,
                "Нечего продолжать",
                "Начните фильм или сериал — он появится здесь",
            )
        }
        items(state.continueWatching, key = { it.itemId }) { entry ->
            FilmaxContinueCard(
                title = entry.title,
                meta = continueMeta(entry.progress),
                imageUrl = entry.wideOrPoster,
                progress = entry.progress?.fraction ?: 0f,
                onClick = { actions.play(entry) },
                onLongClick = { actions.onConfirm(MineConfirm.History(entry.itemId, entry.title)) },
            )
        }
    }
}

@Composable
private fun WatchlistPage(state: MineState, actions: MineActions, contentPadding: PaddingValues) {
    MineGrid(columns = FilmaxMetrics.POSTER_COLUMNS, contentPadding = contentPadding) {
        if (state.watchlist.isNotEmpty()) {
            sortChip(state.watchlistSort.label) { actions.onEvent(MineEvent.CycleWatchlistSort) }
        }
        when {
            state.loading && state.watchlist.isEmpty() -> loading()
            state.watchlist.isEmpty() -> empty(
                Icons.Filled.Bookmarks,
                "Пока пусто",
                "Отмечайте «Буду смотреть» в карточке тайтла",
            )
        }
        items(state.watchlist, key = { it.id }) { item ->
            FilmaxPosterCard(
                title = item.title,
                posterUrl = item.posterSmall,
                onClick = { actions.onOpenItem(item.id) },
                rating = ratingLabel(item.rating),
                meta = item.year.takeIf { it > 0 }?.toString(),
                onLongClick = { actions.onConfirm(MineConfirm.Watchlist(item.id, item.title)) },
            )
        }
    }
}

@Composable
private fun FoldersPage(state: MineState, actions: MineActions, contentPadding: PaddingValues) {
    MineGrid(columns = FilmaxMetrics.WIDE_COLUMNS, contentPadding = contentPadding) {
        if (state.folders.isNotEmpty()) {
            sortChip(state.folderSort.label) { actions.onEvent(MineEvent.CycleFolderSort) }
        }
        item(key = "create") { FilmaxCreateFolderCard(onClick = { actions.onEvent(MineEvent.StartCreateFolder) }) }
        items(state.folders, key = { it.folder.id }) { mine ->
            FilmaxFolderCard(
                name = mine.folder.title,
                count = titlesLabel(mine.folder.count),
                hue = folderHue(mine.folder.tint),
                posters = mine.posters,
                onClick = { actions.onOpenFolder(mine.folder.id) },
            )
        }
    }
}

/** История по дням; «Очистить историю» — над ней, с подтверждением: вернуть её будет нечем. */
@Composable
private fun HistoryPage(state: MineState, actions: MineActions, contentPadding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding()
        ),
    ) {
        if (state.history.isEmpty()) {
            item(key = "empty") {
                if (state.loading) {
                    Loading()
                } else {
                    FilmaxEmptyState(
                        Icons.Filled.History,
                        "История пуста",
                        subtitle = "Здесь будет всё, что вы смотрели",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    )
                }
            }
        } else {
            item(
                key = "clear"
            ) { ClearHistory(contentPadding, onClick = { actions.onEvent(MineEvent.AskClearHistory) }) }
        }
        state.history.forEach { day ->
            item(key = "day-${day.label}") {
                Text(
                    day.label,
                    style = FilmaxType.playerTitle,
                    color = FilmaxColors.Text,
                    modifier = Modifier.padding(start = FilmaxMetrics.ScreenPadding, top = 16.dp, bottom = 6.dp),
                )
            }
            items(day.entries, key = { "history-${day.label}-${it.entry.itemId}" }) { (entry, time) ->
                FilmaxHistoryRow(
                    title = entry.title,
                    subtitle = historySubtitle(entry.progress),
                    meta = historyMeta(entry.progress, time),
                    imageUrl = entry.wideOrPoster,
                    progress = entry.progress?.fraction ?: 0f,
                    onClick = { actions.play(entry) },
                    onMore = { actions.onConfirm(MineConfirm.History(entry.itemId, entry.title)) },
                )
            }
        }
    }
}

@Composable
private fun MineGrid(columns: Int, contentPadding: PaddingValues, content: LazyGridScope.() -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(FilmaxMetrics.CardGap),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        content = content,
    )
}

/** Чип сортировки над сеткой: тап — следующий порядок по кругу. */
private fun LazyGridScope.sortChip(label: String, onClick: () -> Unit) {
    item(key = "sort", span = { GridItemSpan(maxLineSpan) }) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            FilmaxChip(label = label, selected = false, onClick = onClick, leadingIcon = Icons.Filled.SwapVert)
        }
    }
}

private fun LazyGridScope.loading() {
    item(key = "loading", span = { GridItemSpan(maxLineSpan) }) { Loading() }
}

private fun LazyGridScope.empty(icon: ImageVector, title: String, subtitle: String) {
    item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
        FilmaxEmptyState(icon, title, subtitle = subtitle, modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp))
    }
}

@Composable
private fun Loading() {
    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = FilmaxTheme.palette.accent)
    }
}

/** «Продолжить» и история ведут сразу в плеер: недосмотренная серия — сезон и номер из истории. */
private fun MineActions.play(entry: WatchHistory) {
    onPlay(entry.itemId, entry.progress?.season ?: NO_EPISODE, entry.progress?.videoId ?: NO_EPISODE)
}

/** «Очистить историю» над днями — справа, на полях сетки. */
@Composable
private fun ClearHistory(contentPadding: PaddingValues, onClick: () -> Unit) {
    val direction = LocalLayoutDirection.current
    Box(
        Modifier.fillMaxWidth().padding(
            start = contentPadding.calculateStartPadding(direction),
            end = contentPadding.calculateEndPadding(direction),
        ),
        contentAlignment = Alignment.CenterEnd,
    ) {
        FilmaxTextButton(text = "Очистить историю", onClick = onClick)
    }
}

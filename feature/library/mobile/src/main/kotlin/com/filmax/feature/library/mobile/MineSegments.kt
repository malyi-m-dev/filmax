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
import com.filmax.core.presentation.labels.continueMeta
import com.filmax.core.presentation.labels.ratingLabel
import com.filmax.core.presentation.labels.titlesLabel
import com.filmax.core.ui.components.FilmaxChip
import com.filmax.core.ui.components.FilmaxContinueCard
import com.filmax.core.ui.components.FilmaxCreateFolderCard
import com.filmax.core.ui.components.FilmaxEmptyState
import com.filmax.core.ui.components.FilmaxFolderCard
import com.filmax.core.ui.components.FilmaxHistoryRow
import com.filmax.core.ui.components.FilmaxPosterCard
import com.filmax.core.ui.components.FilmaxTextButton
import com.filmax.core.ui.components.folderHue
import com.filmax.feature.library.common.HistoryEntry
import com.filmax.feature.library.common.MineEvent
import com.filmax.feature.library.common.MineRemoval
import com.filmax.feature.library.common.MineSegment
import com.filmax.feature.library.common.MineState

@Composable
internal fun MineSegmentPage(
    segment: MineSegment,
    state: MineState,
    onEvent: (MineEvent) -> Unit,
    contentPadding: PaddingValues
) {
    when (segment) {
        MineSegment.CONTINUE -> ContinuePage(state, onEvent, contentPadding)
        MineSegment.WATCHLIST -> WatchlistPage(state, onEvent, contentPadding)
        MineSegment.FOLDERS -> FoldersPage(state, onEvent, contentPadding)
        MineSegment.HISTORY -> HistoryPage(state, onEvent, contentPadding)
    }
}

@Composable
private fun ContinuePage(state: MineState, onEvent: (MineEvent) -> Unit, contentPadding: PaddingValues) {
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
                onClick = { onEvent(MineEvent.ContinueWatching(entry)) },
                onLongClick = { onEvent(MineEvent.AskRemoval(MineRemoval.History(entry.itemId, entry.title))) },
            )
        }
    }
}

@Composable
private fun WatchlistPage(state: MineState, onEvent: (MineEvent) -> Unit, contentPadding: PaddingValues) {
    MineGrid(columns = FilmaxMetrics.POSTER_COLUMNS, contentPadding = contentPadding) {
        if (state.watchlist.isNotEmpty()) {
            sortChip(state.watchlistSort.label) { onEvent(MineEvent.CycleWatchlistSort) }
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
                onClick = { onEvent(MineEvent.OpenItem(item.id)) },
                rating = ratingLabel(item.rating),
                meta = item.year.takeIf { it > 0 }?.toString(),
                onLongClick = { onEvent(MineEvent.AskRemoval(MineRemoval.Watchlist(item.id, item.title))) },
            )
        }
    }
}

@Composable
private fun FoldersPage(state: MineState, onEvent: (MineEvent) -> Unit, contentPadding: PaddingValues) {
    MineGrid(columns = FilmaxMetrics.WIDE_COLUMNS, contentPadding = contentPadding) {
        if (state.folders.isNotEmpty()) {
            sortChip(state.folderSort.label) { onEvent(MineEvent.CycleFolderSort) }
        }
        item(key = "create") { FilmaxCreateFolderCard(onClick = { onEvent(MineEvent.StartCreateFolder) }) }
        items(state.folders, key = { it.folder.id }) { mine ->
            FilmaxFolderCard(
                name = mine.folder.title,
                count = titlesLabel(mine.folder.count),
                hue = folderHue(mine.folder.tint),
                posters = mine.posters,
                onClick = { onEvent(MineEvent.OpenFolder(mine.folder.id)) },
            )
        }
    }
}

/** История по дням; «Очистить историю» — над ней, с подтверждением: вернуть её будет нечем. */
@Composable
private fun HistoryPage(state: MineState, onEvent: (MineEvent) -> Unit, contentPadding: PaddingValues) {
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
            item(key = "clear") { ClearHistory(contentPadding, onClick = { onEvent(MineEvent.AskClearHistory) }) }
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
            items(day.entries, key = { "history-${day.label}-${it.entry.itemId}" }) { entry ->
                HistoryRow(entry, onEvent)
            }
        }
    }
}

/** Строка истории: «S4E7» или «Фильм», когда и сколько — «21:10 · на 45%». */
@Composable
private fun HistoryRow(history: HistoryEntry, onEvent: (MineEvent) -> Unit) {
    val entry = history.entry
    FilmaxHistoryRow(
        title = entry.title,
        subtitle = history.episodeCode ?: "Фильм",
        meta = listOfNotNull(history.time, history.watchedShare).joinToString(" · ").ifBlank { null },
        imageUrl = entry.wideOrPoster,
        progress = entry.progress?.fraction ?: 0f,
        onClick = { onEvent(MineEvent.ContinueWatching(entry)) },
        onMore = { onEvent(MineEvent.AskRemoval(MineRemoval.History(entry.itemId, entry.title))) },
    )
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

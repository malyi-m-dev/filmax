package com.filmax.feature.library.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmax.core.domain.downloads.model.DownloadedItem
import com.filmax.core.domain.favorites.model.FavoriteItem
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.ui.components.FilmaxEmptyState
import com.filmax.core.ui.components.PosterImage
import com.filmax.feature.library.common.LibraryEvent
import com.filmax.feature.library.common.LibraryScreenModel
import com.filmax.feature.library.common.LibraryState
import com.filmax.feature.library.common.LibraryTab
import org.koin.androidx.compose.koinViewModel

@Composable
fun LibraryScreen(
    onOpenItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
    screenModel: LibraryScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp)) {
            Text(
                "Моя библиотека",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Избранное, загрузки и история просмотров",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LibraryTab.entries.forEach { tab ->
                LibraryTabPill(
                    tab = tab,
                    selected = state.tab == tab,
                    count = countFor(tab, state),
                    onClick = { screenModel.dispatch(LibraryEvent.TabChange(tab)) },
                )
            }
        }

        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            when (state.tab) {
                LibraryTab.FAVORITES -> FavoritesTab(
                    state = state,
                    onOpenItem = onOpenItem,
                )
                LibraryTab.HISTORY -> HistoryTab(
                    state = state,
                    onOpenItem = onOpenItem,
                    onRemove = { screenModel.dispatch(LibraryEvent.RemoveFromHistory(it)) },
                    onClear = { screenModel.dispatch(LibraryEvent.ClearHistory) },
                )
                LibraryTab.DOWNLOADS -> DownloadsTab(state = state, onOpenItem = onOpenItem)
                LibraryTab.LISTS -> ListsTab(state = state)
            }
        }
    }
}

@Composable
private fun FavoritesTab(state: LibraryState, onOpenItem: (Int) -> Unit) {
    if (state.favorites.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Bookmarks,
            text = "Добавляйте фильмы в избранное",
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.favorites, key = { it.id }) { favorite ->
                FavoriteCard(item = favorite, onClick = { onOpenItem(favorite.id) })
            }
        }
    }
}

@Composable
private fun FavoriteCard(item: FavoriteItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        PosterImage(
            url = item.posterSmall,
            contentDescription = item.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            item.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
        )
        Text(
            "${item.year} · ${item.durationMinutes} мин",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun HistoryTab(
    state: LibraryState,
    onOpenItem: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onClear: () -> Unit,
) {
    if (state.history.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.PlayCircleOutline,
            text = "История просмотров пуста",
        )
    } else {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onClear) {
                    Text("Очистить всё", fontSize = 12.sp)
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.history, key = { it.itemId }) { entry ->
                    HistoryRow(
                        entry = entry,
                        onClick = { onOpenItem(entry.itemId) },
                        onRemove = { onRemove(entry.itemId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: WatchHistory, onClick: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PosterImage(
            url = entry.posterSmall.orEmpty(),
            contentDescription = entry.title,
            modifier = Modifier
                .size(width = 60.dp, height = 88.dp)
                .clip(RoundedCornerShape(10.dp)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Spacer(Modifier.height(6.dp))
            val fraction = entry.progress?.fraction ?: 0f
            LinearProgressIndicator(
                progress = { fraction },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${(fraction * 100).toInt()}%",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Удалить",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ListsTab(state: LibraryState) {
    if (state.lists.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Folder,
            text = "Создайте список для организации контента",
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.lists, key = { it.id }) { folder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            folder.title,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "${folder.count} элементов",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadsTab(state: LibraryState, onOpenItem: (Int) -> Unit) {
    if (state.downloads.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Download,
            text = "Скачанные фильмы появятся здесь",
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.downloads, key = { it.id }) { dl ->
                DownloadRow(item = dl, onClick = { onOpenItem(dl.id) })
            }
        }
    }
}

@Composable
private fun DownloadRow(item: DownloadedItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PosterImage(
            url = item.posterSmall,
            contentDescription = item.title,
            modifier = Modifier
                .size(width = 56.dp, height = 80.dp)
                .clip(RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${item.year} · ${item.durationMinutes} мин",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color(0xFF6AC2B0),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    "В библиотеке",
                    fontSize = 11.sp,
                    color = Color(0xFF6AC2B0),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun LibraryTabPill(
    tab: LibraryTab,
    selected: Boolean,
    count: Int,
    onClick: () -> Unit,
) {
    val icon = when (tab) {
        LibraryTab.FAVORITES -> Icons.Filled.Favorite
        LibraryTab.HISTORY -> Icons.Filled.History
        LibraryTab.DOWNLOADS -> Icons.Filled.Download
        LibraryTab.LISTS -> Icons.AutoMirrored.Filled.List
    }
    val shape = RoundedCornerShape(percent = 50)
    val fg = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .then(if (!selected) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        Text(tab.label, color = fg, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Box(
            modifier = Modifier
                .clip(shape)
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                )
                .padding(horizontal = 7.dp, vertical = 2.dp),
        ) {
            Text("$count", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
        }
    }
}

private fun countFor(tab: LibraryTab, state: LibraryState): Int = when (tab) {
    LibraryTab.FAVORITES -> state.favorites.size
    LibraryTab.HISTORY -> state.history.size
    LibraryTab.DOWNLOADS -> state.downloads.size
    LibraryTab.LISTS -> state.lists.size
}

@Composable
private fun EmptyState(icon: ImageVector, text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        FilmaxEmptyState(icon = icon, title = text)
    }
}

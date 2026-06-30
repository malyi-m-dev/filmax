package com.filmax.feature.library.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmax.core.tv.designsystem.ScrollToTopOnNavFocus
import com.filmax.core.tv.designsystem.TvFocusCard
import com.filmax.core.tv.designsystem.TvPosterTitle
import com.filmax.core.ui.components.PosterImage
import com.filmax.feature.library.common.LibraryEvent
import com.filmax.feature.library.common.LibraryScreenModel
import com.filmax.feature.library.common.LibraryTab
import org.koin.androidx.compose.koinViewModel

private val Accent = Color(0xFFB4305A)

/** Лёгкая карточка для сетки: постер + id + название. */
private data class Tile(val id: Int, val title: String, val poster: String)

/**
 * TV-Библиотека (экран 04 макета): чипы-табы + сетка постеров. Поверх общего
 * [LibraryScreenModel] (избранное/история/загрузки/списки — те же данные, что и на телефоне).
 */
@Composable
fun TvLibraryScreen(
    onOpenItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
    screenModel: LibraryScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()

    val tiles: List<Tile> = when (state.tab) {
        LibraryTab.FAVORITES -> state.favorites.map { Tile(it.id, it.title, it.posterSmall) }
        LibraryTab.HISTORY -> state.history.map { Tile(it.itemId, it.title, it.posterSmall.orEmpty()) }
        LibraryTab.DOWNLOADS -> state.downloads.map { Tile(it.id, it.title, it.posterSmall) }
        LibraryTab.LISTS -> emptyList()
    }

    // В композиции единовременно только одна сетка (LISTS либо остальные) — общий state безопасен.
    val gridState = rememberLazyGridState()
    ScrollToTopOnNavFocus(gridState)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 72.dp, end = 72.dp, top = 120.dp),
    ) {
        Text(
            "Моя библиотека",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(24.dp))

        // ── Табы ────────────────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LibraryTab.entries.forEach { tab ->
                val count = when (tab) {
                    LibraryTab.FAVORITES -> state.favorites.size
                    LibraryTab.HISTORY -> state.history.size
                    LibraryTab.DOWNLOADS -> state.downloads.size
                    LibraryTab.LISTS -> state.lists.size
                }
                TabChip(
                    label = tab.label,
                    count = count,
                    active = state.tab == tab,
                    onClick = { screenModel.dispatch(LibraryEvent.TabChange(tab)) },
                )
            }
        }
        Spacer(Modifier.height(32.dp))

        // ── Контент ─────────────────────────────────────────────────────
        if (state.tab == LibraryTab.LISTS) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = gridState,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 40.dp),
            ) {
                items(state.lists, key = { it.id }) { folder ->
                    FolderTile(title = folder.title, count = folder.count)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                state = gridState,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 40.dp),
            ) {
                if (tiles.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "Здесь пока пусто",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(vertical = 40.dp),
                        )
                    }
                } else {
                    items(tiles, key = { it.id }) { tile ->
                        PosterTile(tile = tile, onClick = { onOpenItem(tile.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TabChip(label: String, count: Int, active: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(percent = 50)
    TvFocusCard(onClick = onClick, shape = shape) {
        Row(
            modifier = Modifier
                .clip(shape)
                .background(
                    if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                )
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                label,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "$count",
                fontSize = 13.sp,
                color = if (active) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(
                        alpha = 0.8f
                    )
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun PosterTile(tile: Tile, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    TvFocusCard(onClick = onClick, shape = shape, modifier = Modifier.height(240.dp)) {
        Box(Modifier.fillMaxSize()) {
            PosterImage(
                url = tile.poster,
                contentDescription = tile.title,
                modifier = Modifier.fillMaxSize(),
                shape = shape,
                accentColor = Accent,
            )
            TvPosterTitle(title = tile.title)
        }
    }
}

@Composable
private fun FolderTile(title: String, count: Int) {
    val shape = RoundedCornerShape(24.dp)
    TvFocusCard(onClick = {}, shape = shape, modifier = Modifier.height(140.dp)) {
        Column(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("$count элементов", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

package com.filmax.feature.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.ui.components.PosterImage

private val CollectionPalette = listOf(
    Color(0xFFD4A84A),
    Color(0xFF1E88E5),
    Color(0xFFE86D9E),
    Color(0xFF6AC2B0),
    Color(0xFFC67A3E),
    Color(0xFF6B4B8F),
)

private val CollectionIcons = listOf(
    Icons.Filled.Star,
    Icons.Filled.LocalFireDepartment,
    Icons.Filled.TrendingUp,
    Icons.Filled.AutoAwesome,
    Icons.Filled.Visibility,
)

private fun accentFor(index: Int): Color = CollectionPalette[index % CollectionPalette.size]
private fun iconFor(index: Int): ImageVector = CollectionIcons[index % CollectionIcons.size]

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(
    onOpenItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollectionsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding(),
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)) {
            Text(
                "Подборки",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Кураторские коллекции фильмов и сериалов",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ── Search ────────────────────────────────────────────────────────────
        SearchField(
            query = state.query,
            onQueryChange = viewModel::onQueryChange,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            else -> CollectionsList(
                state = state,
                onCollectionClick = viewModel::onCollectionClick,
            )
        }
    }

    val selected = state.selectedCollection
    if (selected != null) {
        val selectedIndex = state.collections.indexOf(selected).coerceAtLeast(0)
        ModalBottomSheet(
            onDismissRequest = viewModel::onDismissSheet,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline) },
        ) {
            CollectionSheet(
                collection = selected,
                accent = accentFor(selectedIndex),
                icon = iconFor(selectedIndex),
                loading = state.loadingItems,
                items = state.collectionItems,
                onOpenItem = { id ->
                    viewModel.onDismissSheet()
                    onOpenItem(id)
                },
            )
        }
    }
}

@Composable
private fun CollectionsList(
    state: CollectionsUiState,
    onCollectionClick: (Collection) -> Unit,
) {
    val filtered = state.filtered
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        // Featured "collection of the week" — full width, only when not searching
        val featured = state.collections.firstOrNull()
        if (state.query.isBlank() && featured != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                FeaturedCollectionCard(
                    collection = featured,
                    accent = accentFor(0),
                    onClick = { onCollectionClick(featured) },
                )
            }
        }

        items(filtered, key = { it.id }) { collection ->
            val index = state.collections.indexOf(collection).coerceAtLeast(0)
            CollectionCard(
                collection = collection,
                accent = accentFor(index),
                icon = iconFor(index),
                onClick = { onCollectionClick(collection) },
            )
        }

        if (filtered.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Ничего не найдено по «${state.query}»",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    "Поиск по подборкам…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable { onQueryChange("") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Очистить",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun FeaturedCollectionCard(
    collection: Collection,
    accent: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.4f))))
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0x40000000))
                .padding(horizontal = 12.dp, vertical = 5.dp),
        ) {
            Text(
                "★ ПОДБОРКА НЕДЕЛИ",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp,
                color = Color.White,
            )
        }
        Column(Modifier.align(Alignment.BottomStart)) {
            Text(
                collection.title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            collection.description?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    it,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CollectionCard(
    collection: Collection,
    accent: Color,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        listOf(accent.copy(alpha = 0.28f), accent.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        )
        Column(
            modifier = Modifier
                .matchParentSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(
                    collection.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                collection.description?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        it,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionSheet(
    collection: Collection,
    accent: Color,
    icon: ImageVector,
    loading: Boolean,
    items: List<Item>,
    onOpenItem: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    collection.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                collection.description?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (loading) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onOpenItem(item.id) },
                    ) {
                        PosterImage(
                            url = item.posters.medium,
                            contentDescription = item.title,
                            modifier = Modifier.matchParentSize(),
                            accentColor = accent,
                        )
                    }
                }
            }
        }
    }
}

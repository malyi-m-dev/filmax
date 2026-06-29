package com.filmax.feature.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel
import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.ui.components.FilmaxCollectionCard
import com.filmax.core.ui.components.FilmaxErrorModal
import com.filmax.core.ui.components.FilmaxSearchField
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

@Composable
fun CollectionsScreen(
    onOpenCollection: (Int, String) -> Unit,
    modifier: Modifier = Modifier,
    screenModel: CollectionsScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()
    val appError by screenModel.collectErrorAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding(),
    ) {
        Column(Modifier.fillMaxSize()) {
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
            FilmaxSearchField(
                query = state.query,
                onQueryChange = { screenModel.dispatch(CollectionsEvent.QueryChange(it)) },
                placeholder = "Поиск по подборкам…",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )

            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

                else -> CollectionsList(
                    state = state,
                    onCollectionClick = { onOpenCollection(it.id, it.title) },
                )
            }
        }

        appError?.let { error ->
            FilmaxErrorModal(
                error = error,
                onDismiss = screenModel::dismissError,
                onPrimary = screenModel::retry,
            )
        }
    }
}

@Composable
private fun CollectionsList(
    state: CollectionsState,
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
            FilmaxCollectionCard(
                title = collection.title,
                subtitle = collection.description,
                accent = accentFor(index),
                icon = iconFor(index),
                posterUrl = collection.posters?.medium ?: collection.posters?.big,
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
private fun FeaturedCollectionCard(
    collection: Collection,
    accent: Color,
    onClick: () -> Unit,
) {
    val poster = collection.posters?.big ?: collection.posters?.medium
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.4f))))
            .clickable(onClick = onClick),
    ) {
        if (!poster.isNullOrBlank()) {
            PosterImage(
                url = poster,
                contentDescription = collection.title,
                modifier = Modifier.matchParentSize(),
                shape = RoundedCornerShape(28.dp),
                accentColor = accent,
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(accent.copy(alpha = 0.5f), Color.Transparent, Color(0xF2141012))
                        )
                    )
            )
        }
        Box(
            modifier = Modifier
                .padding(20.dp)
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
        Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
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


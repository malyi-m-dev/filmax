package com.filmax.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.Duration
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemRating
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.Posters
import com.filmax.core.ui.components.ContinueCard
import com.filmax.core.ui.components.FavButton
import com.filmax.core.ui.components.HorizontalRow
import com.filmax.core.ui.components.PosterCard
import com.filmax.core.ui.components.PosterImage
import com.filmax.core.ui.components.RatingPill

private val SectionPalette = listOf(
    Color(0xFFB4305A), Color(0xFFE86D9E), Color(0xFFF4B792), Color(0xFF6AC2B0), Color(0xFF6B4B8F),
)

@Composable
fun HomeScreen(
    onOpenItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error ?: "Ошибка", color = MaterialTheme.colorScheme.error)
            }

            else -> HomeContent(
                state = state,
                onOpenItem = onOpenItem,
                onFav = viewModel::toggleFav,
            )
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onOpenItem: (Int) -> Unit,
    onFav: (Int) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 120.dp),
        ) {
            // Reserve space for the pinned top bar (status bar inset + bar height)
            Spacer(Modifier.statusBarsPadding().height(72.dp))

            // ── Hero ───────────────────────────────────────────────────────────
            state.hero?.let { hero ->
                HeroCard(
                    item = hero,
                    isFav = hero.id in state.favorites,
                    onClick = { onOpenItem(hero.id) },
                    onFav = { onFav(hero.id) },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 28.dp),
                )
            }

            // ── Continue watching — 2-column grid ───────────────────────────────
            if (state.continueWatching.isNotEmpty()) {
                SectionHeader(title = "Продолжить просмотр", accent = Color(0xFFF4B792))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    state.continueWatching.take(2).forEach { history ->
                        ContinueCard(
                            item = history.toItem(),
                            progress = history.progress?.fraction ?: 0f,
                            onClick = { onOpenItem(history.itemId) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (state.continueWatching.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            // ── Collections ─────────────────────────────────────────────────────
            if (state.collections.isNotEmpty()) {
                CollectionsSection(collections = state.collections)
            }

            // ── Trending ────────────────────────────────────────────────────────
            if (state.trending.isNotEmpty()) {
                HorizontalRow(
                    title = "В тренде",
                    accentColor = Color(0xFFB4305A),
                    modifier = Modifier.padding(bottom = 28.dp),
                ) {
                    state.trending.forEach { item ->
                        PosterCard(
                            item = item,
                            isFav = item.id in state.favorites,
                            onClick = { onOpenItem(item.id) },
                            onFavClick = { onFav(item.id) },
                        )
                    }
                }
            }

            // ── For you ─────────────────────────────────────────────────────────
            if (state.forYou.isNotEmpty()) {
                HorizontalRow(
                    title = "Для вас",
                    subtitle = "На основе ваших предпочтений",
                    accentColor = Color(0xFF6AC2B0),
                    modifier = Modifier.padding(bottom = 28.dp),
                ) {
                    state.forYou.forEach { item ->
                        PosterCard(
                            item = item,
                            isFav = item.id in state.favorites,
                            onClick = { onOpenItem(item.id) },
                            onFavClick = { onFav(item.id) },
                        )
                    }
                }
            }
        }

        // Pinned, edge-to-edge top bar with frosted gradient
        HomeTopBar(modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun HomeTopBar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        Color.Transparent,
                    )
                )
            )
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Добрый вечер",
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Filmax", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(".", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFFB4305A), Color(0xFFF4B792)))),
                contentAlignment = Alignment.Center,
            ) {
                Text("АК", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    accent: Color,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 14.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .width(4.dp)
                        .height(22.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accent),
                )
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            }
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp, top = 2.dp),
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 2.dp),
        )
    }
}

@Composable
private fun CollectionsSection(collections: List<Collection>) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, collections) {
        if (query.isBlank()) collections
        else collections.filter {
            it.title.contains(query, ignoreCase = true) ||
                (it.description?.contains(query, ignoreCase = true) == true)
        }
    }

    Column(Modifier.padding(bottom = 28.dp)) {
        SectionHeader(title = "Подборки", accent = Color(0xFFE86D9E))

        // Inline search
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 14.dp)
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("Поиск по подборкам…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable { query = "" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Очистить", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                }
            }
        }

        // 2-column grid
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            filtered.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    rowItems.forEachIndexed { i, c ->
                        CollectionCard(
                            collection = c,
                            accent = SectionPalette[(c.id + i) % SectionPalette.size],
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            if (filtered.isEmpty()) {
                Text(
                    "Ничего не найдено",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun CollectionCard(
    collection: Collection,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(150.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        val poster = collection.posters?.medium?.takeIf { it.isNotBlank() }
            ?: collection.posters?.big?.takeIf { it.isNotBlank() }
        if (poster != null) {
            PosterImage(
                url = poster,
                contentDescription = collection.title,
                modifier = Modifier.matchParentSize(),
                shape = RoundedCornerShape(0.dp),
                accentColor = accent,
            )
        }
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        listOf(accent.copy(alpha = 0.85f), accent.copy(alpha = 0.3f), Color.Transparent)
                    )
                )
        )
        Column(
            modifier = Modifier
                .matchParentSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                collection.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            collection.description?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(3.dp))
                Text(
                    it,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun com.filmax.core.domain.watching.model.WatchHistory.toItem(): Item = Item(
    id = itemId,
    title = title,
    type = ItemType.MOVIE,
    year = 0,
    plot = "",
    director = "",
    cast = "",
    country = "",
    genres = emptyList(),
    rating = ItemRating(0, "", null, null),
    posters = Posters(posterSmall ?: "", "", "", null),
    duration = Duration(120.0, null),
    tracklist = emptyList(),
    trailer = null,
    inWatchlist = false,
    finished = false,
)

@Composable
private fun HeroCard(
    item: Item,
    isFav: Boolean,
    onClick: () -> Unit,
    onFav: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(440.dp)
            .clip(RoundedCornerShape(32.dp))
            .clickable(onClick = onClick),
    ) {
        PosterImage(
            url = item.posters.big,
            contentDescription = item.title,
            modifier = Modifier.matchParentSize(),
            shape = RoundedCornerShape(32.dp),
            accentColor = Color(0xFFB4305A),
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1f to Color(0xFF141012).copy(alpha = 0.95f),
                        ),
                    )
                )
        )
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xFFB4305A),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
        ) {
            Text(
                "🔥 Выбор редакции",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                color = Color.White, letterSpacing = 1.sp,
            )
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            RatingPill(rating = item.rating.filmax / 10f)
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
        ) {
            Text(
                item.genres.take(2).joinToString(" · ") { it.title } + " · ${item.year}",
                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.8f), letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                item.title,
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(shape = RoundedCornerShape(50), color = Color.White) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                        Text("Смотреть", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
                FavButton(isFav = isFav, onClick = onFav, modifier = Modifier.size(48.dp))
            }
        }
    }
}

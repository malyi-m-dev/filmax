package com.filmax.feature.home.mobile

import com.filmax.feature.home.HomeScreenModel
import com.filmax.feature.home.HomeState
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel
import com.filmax.core.domain.catalog.model.Duration
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemRating
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.Posters
import com.filmax.core.ui.components.ContinueCard
import com.filmax.core.ui.components.FilmaxErrorModal
import com.filmax.core.ui.components.HorizontalRow
import com.filmax.core.ui.components.PosterCard
import com.filmax.core.ui.components.PosterImage
import com.filmax.core.ui.components.RatingPill

@Composable
fun HomeScreen(
    onOpenItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
    screenModel: HomeScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()
    val appError by screenModel.collectErrorAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            else -> HomeContent(
                state = state,
                onOpenItem = onOpenItem,
            )
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
private fun HomeContent(
    state: HomeState,
    onOpenItem: (Int) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 120.dp),
        ) {
            // Reserve space for the pinned top bar (status bar inset + bar height)
            Spacer(Modifier.statusBarsPadding().height(60.dp))

            // ── Hero ───────────────────────────────────────────────────────────
            state.hero?.let { hero ->
                HeroCard(
                    item = hero,
                    onClick = { onOpenItem(hero.id) },
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
                            onClick = { onOpenItem(item.id) },
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
                            onClick = { onOpenItem(item.id) },
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
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
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
    onClick: () -> Unit,
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
        }
    }
}

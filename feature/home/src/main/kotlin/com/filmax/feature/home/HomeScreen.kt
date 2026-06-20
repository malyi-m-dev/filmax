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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.ui.components.ContinueCard
import com.filmax.core.ui.components.FavButton
import com.filmax.core.ui.components.HorizontalRow
import com.filmax.core.ui.components.PosterCard
import com.filmax.core.ui.components.PosterImage
import com.filmax.core.ui.components.RatingPill

@Composable
fun HomeScreen(
    onOpenItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surface)) {
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            state.error != null -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 120.dp),
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            Color.Transparent,
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Добрый вечер",
                        fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "Filmax",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            ".",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = "Уведомления",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.width(8.dp))
                // Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFFB4305A),
                                    Color(0xFFF4B792)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("АК", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        // ── Hero ─────────────────────────────────────────────────────────────
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

        // ── Continue watching ─────────────────────────────────────────────────
        if (state.continueWatching.isNotEmpty()) {
            HorizontalRow(
                title = "Продолжить просмотр",
                accentColor = Color(0xFFF4B792),
                modifier = Modifier.padding(bottom = 28.dp),
            ) {
                state.continueWatching.forEachIndexed { i, history ->
                    ContinueCard(
                        item = Item(
                            id = history.itemId, title = history.title,
                            type = com.filmax.core.domain.catalog.model.ItemType.MOVIE,
                            year = 0, plot = "", director = "", cast = "", country = "",
                            genres = emptyList(),
                            rating = com.filmax.core.domain.catalog.model.ItemRating(
                                0,
                                "",
                                null,
                                null
                            ),
                            posters = com.filmax.core.domain.catalog.model.Posters(
                                history.posterSmall ?: "", "", "", null
                            ),
                            duration = com.filmax.core.domain.catalog.model.Duration(120.0, null),
                            tracklist = emptyList(), trailer = null,
                            inWatchlist = false, finished = false,
                        ),
                        progress = history.progress?.fraction ?: 0f,
                        onClick = { onOpenItem(history.itemId) },
                    )
                }
            }
        }

        // ── Trending ──────────────────────────────────────────────────────────
        if (state.trending.isNotEmpty()) {
            HorizontalRow(
                title = "В тренде",
                accentColor = MaterialTheme.colorScheme.primaryContainer,
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

        // ── For you ───────────────────────────────────────────────────────────
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
}

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
        // Poster
        PosterImage(
            url = item.posters.big,
            contentDescription = item.title,
            modifier = Modifier.matchParentSize(),
            shape = RoundedCornerShape(32.dp),
            accentColor = Color(0xFFB4305A),
        )
        // Gradient overlay
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1f to Color(0xFF141012).copy(alpha = 0.95f)
                        ),
                    )
                )
        )
        // "Выбор редакции" badge
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
        // Rating
        Box(Modifier
            .align(Alignment.TopEnd)
            .padding(16.dp)) {
            RatingPill(rating = item.rating.filmax / 10f)
        }
        // Bottom info
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
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Watch button
                Surface(shape = RoundedCornerShape(50), color = Color.White) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Смотреть",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
                FavButton(isFav = isFav, onClick = onFav, modifier = Modifier.size(48.dp))
            }
        }
    }
}

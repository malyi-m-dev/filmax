package com.filmax.feature.home.tv

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.tv.designsystem.TvButton
import com.filmax.core.tv.designsystem.TvFocusCard
import com.filmax.core.ui.components.PosterImage
import com.filmax.core.ui.components.RatingPill
import com.filmax.feature.home.HomeScreenModel
import com.filmax.feature.home.HomeState
import org.koin.androidx.compose.koinViewModel

private val Accent = Color(0xFFB4305A)

/**
 * TV-Главная (экран 01 макета): hero-бэкдроп с действиями и горизонтальные рельсы постеров.
 * Поверх общего [HomeScreenModel] — данные те же, что и на телефоне. Верхний таб-бар рисует
 * общий скаффолд `app-tv`. Фокус/скролл — нативные (tv-material3 + Lazy bring-into-view).
 */
@Composable
fun TvHomeScreen(
    onOpenItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
    screenModel: HomeScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            else -> TvHomeContent(state = state, onOpenItem = onOpenItem)
        }
    }
}

@Composable
private fun TvHomeContent(state: HomeState, onOpenItem: (Int) -> Unit) {
    val playFocus = remember { FocusRequester() }
    LaunchedEffect(state.hero?.id) {
        if (state.hero != null) runCatching { playFocus.requestFocus() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 120.dp, bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(36.dp),
    ) {
        state.hero?.let { hero ->
            item(key = "hero") {
                TvHero(
                    item = hero,
                    playFocusRequester = playFocus,
                    onPlay = { onOpenItem(hero.id) },
                    onDetails = { onOpenItem(hero.id) },
                )
            }
        }

        if (state.continueWatching.isNotEmpty()) {
            item(key = "continue") {
                TvRail(title = "Продолжить просмотр") {
                    items(state.continueWatching, key = { it.itemId }) { history ->
                        TvContinueCard(history = history, onClick = { onOpenItem(history.itemId) })
                    }
                }
            }
        }

        if (state.trending.isNotEmpty()) {
            item(key = "trending") {
                TvRail(title = "В тренде") {
                    items(state.trending, key = { it.id }) { itm ->
                        TvPosterCard(item = itm, onClick = { onOpenItem(itm.id) })
                    }
                }
            }
        }

        if (state.forYou.isNotEmpty()) {
            item(key = "forYou") {
                TvRail(title = "Для вас") {
                    items(state.forYou, key = { it.id }) { itm ->
                        TvPosterCard(item = itm, onClick = { onOpenItem(itm.id) })
                    }
                }
            }
        }
    }
}

// ── Hero ──────────────────────────────────────────────────────────────────
@Composable
private fun TvHero(
    item: Item,
    playFocusRequester: FocusRequester,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(540.dp),
    ) {
        PosterImage(
            url = item.posters.wide ?: item.posters.big,
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(0.dp),
            accentColor = Accent,
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        0.45f to MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                        0.8f to Color.Transparent,
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.4f to Color.Transparent,
                        1f to MaterialTheme.colorScheme.surface,
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 72.dp, end = 72.dp, bottom = 8.dp)
                .fillMaxWidth(0.6f),
        ) {
            EditorsChoicePill()
            Spacer(Modifier.height(16.dp))
            Text(
                item.title,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )
            Spacer(Modifier.height(14.dp))
            HeroMeta(item)
            Spacer(Modifier.height(14.dp))
            Text(
                item.plot,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 3,
            )
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TvButton("Смотреть", onClick = onPlay, leadingIcon = Icons.Filled.PlayArrow, focusRequester = playFocusRequester)
                TvButton("В избранное", onClick = {}, primary = false, leadingIcon = Icons.Filled.FavoriteBorder)
                TvButton("Подробнее", onClick = onDetails, primary = false, leadingIcon = Icons.Filled.Info)
            }
        }
    }
}

@Composable
private fun HeroMeta(item: Item) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        RatingPill(rating = item.rating.filmax / 10f)
        MetaText("${item.year}")
        MetaDot()
        item.duration.averageMinutes?.toInt()?.takeIf { it > 0 }?.let {
            MetaText(formatDuration(it)); MetaDot()
        }
        MetaText(item.genres.take(3).joinToString(" · ") { it.title })
    }
}

@Composable
private fun MetaText(text: String) =
    Text(text, fontSize = 16.sp, color = Color.White.copy(alpha = 0.9f))

@Composable
private fun MetaDot() = Box(
    Modifier
        .size(4.dp)
        .clip(CircleShape)
        .background(Color.White.copy(alpha = 0.5f))
)

@Composable
private fun EditorsChoicePill() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(Accent)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            "⚡ ВЫБОР РЕДАКЦИИ",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp,
        )
    }
}

// ── Rails ─────────────────────────────────────────────────────────────────
@Composable
private fun TvRail(title: String, content: LazyListScope.() -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 72.dp, bottom = 16.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 72.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            content = content,
        )
    }
}

@Composable
private fun TvPosterCard(item: Item, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    TvFocusCard(onClick = onClick, shape = shape, modifier = Modifier.size(width = 200.dp, height = 300.dp)) {
        Box(Modifier.fillMaxSize()) {
            PosterImage(
                url = item.posters.medium.ifEmpty { item.posters.big },
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                shape = shape,
                accentColor = Accent,
            )
            RatingPill(
                rating = item.rating.filmax / 10f,
                compact = true,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp),
            )
        }
    }
}

@Composable
private fun TvContinueCard(history: WatchHistory, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(modifier = Modifier.width(320.dp)) {
        TvFocusCard(onClick = onClick, shape = shape, modifier = Modifier.fillMaxWidth().height(180.dp)) {
            Box(Modifier.fillMaxSize()) {
                PosterImage(
                    url = history.posterSmall.orEmpty(),
                    contentDescription = history.title,
                    modifier = Modifier.fillMaxSize(),
                    shape = shape,
                    accentColor = Accent,
                )
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.25f))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(history.progress?.fraction ?: 0f)
                            .height(4.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            history.title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

private fun formatDuration(totalMinutes: Int): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return when {
        h > 0 && m > 0 -> "${h}ч ${m}м"
        h > 0 -> "${h}ч"
        else -> "${m}м"
    }
}

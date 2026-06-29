package com.filmax.feature.details.tv

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
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
import com.filmax.core.tv.designsystem.TvButton
import com.filmax.core.tv.designsystem.TvFocusCard
import com.filmax.core.ui.components.PosterImage
import com.filmax.feature.details.DetailsEvent
import com.filmax.feature.details.DetailsScreenModel
import org.koin.androidx.compose.koinViewModel

private val Accent = Color(0xFFB4305A)

/**
 * TV-Детали (экран 06 макета): бэкдроп + действия + панель «в ролях» + рельса «похожие».
 * Поверх общего [DetailsScreenModel] (itemId берётся из маршрута через SavedStateHandle).
 */
@Composable
fun TvDetailsScreen(
    onPlay: (Int) -> Unit,
    onOpenItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
    screenModel: DetailsScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when {
            state.loading -> CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center),
            )

            state.item != null -> DetailsContent(
                item = state.item!!,
                similar = state.similar,
                isFav = state.isFav,
                onPlay = { onPlay(state.item!!.id) },
                onToggleFav = { screenModel.dispatch(DetailsEvent.ToggleFav) },
                onOpenItem = onOpenItem,
            )
        }
    }
}

@Composable
private fun DetailsContent(
    item: Item,
    similar: List<Item>,
    isFav: Boolean,
    onPlay: () -> Unit,
    onToggleFav: () -> Unit,
    onOpenItem: (Int) -> Unit,
) {
    val playFocus = remember { FocusRequester() }
    LaunchedEffect(item.id) { runCatching { playFocus.requestFocus() } }

    Box(Modifier.fillMaxSize()) {
        // Бэкдроп
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
                        0f to MaterialTheme.colorScheme.surface,
                        0.5f to MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        0.85f to Color.Transparent,
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.45f to Color.Transparent,
                        1f to MaterialTheme.colorScheme.surface,
                    )
                )
        )

        Row(Modifier.fillMaxSize().padding(72.dp)) {
            // Левая колонка — инфо + действия
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Accent)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(item.genres.take(2).joinToString(" · ") { it.title }.uppercase(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                }
                Spacer(Modifier.height(16.dp))
                Text(item.title, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Spacer(Modifier.height(14.dp))
                Text(
                    buildString {
                        append(item.year)
                        item.duration.averageMinutes?.toInt()?.takeIf { it > 0 }?.let { append("  ·  ${it / 60}ч ${it % 60}м") }
                        if (item.country.isNotBlank()) append("  ·  ${item.country}")
                    },
                    fontSize = 16.sp, color = Color.White.copy(alpha = 0.9f),
                )
                Spacer(Modifier.height(16.dp))
                Text(item.plot, fontSize = 18.sp, lineHeight = 26.sp, color = Color.White.copy(alpha = 0.85f), maxLines = 4, modifier = Modifier.fillMaxWidth(0.7f))
                Spacer(Modifier.height(28.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TvButton("Смотреть", onClick = onPlay, leadingIcon = Icons.Filled.PlayArrow, focusRequester = playFocus)
                    TvButton(
                        text = if (isFav) "В избранном" else "В избранное",
                        onClick = onToggleFav,
                        primary = false,
                        leadingIcon = if (isFav) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    )
                }

                Spacer(Modifier.height(28.dp))
                // В ролях / режиссёр
                if (item.cast.isNotBlank()) {
                    Label("В ролях")
                    Text(item.cast, fontSize = 16.sp, color = Color.White.copy(alpha = 0.9f), modifier = Modifier.fillMaxWidth(0.6f))
                }
                if (item.director.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Label("Режиссёр")
                    Text(item.director, fontSize = 16.sp, color = Color.White.copy(alpha = 0.9f))
                }
            }
        }

        // Рельса «похожие» — снизу
        if (similar.isNotEmpty()) {
            Column(Modifier.align(Alignment.BottomStart).padding(start = 72.dp, bottom = 32.dp)) {
                Text("Похожие", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 14.dp))
                LazyRow(
                    contentPadding = PaddingValues(end = 72.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(similar, key = { it.id }) { sim ->
                        SimilarCard(item = sim, onClick = { onOpenItem(sim.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text.uppercase(), color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, modifier = Modifier.padding(bottom = 6.dp))
}

@Composable
private fun SimilarCard(item: Item, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    TvFocusCard(onClick = onClick, shape = shape, modifier = Modifier.size(width = 150.dp, height = 212.dp)) {
        PosterImage(
            url = item.posters.medium.ifEmpty { item.posters.big },
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            shape = shape,
            accentColor = Accent,
        )
    }
}

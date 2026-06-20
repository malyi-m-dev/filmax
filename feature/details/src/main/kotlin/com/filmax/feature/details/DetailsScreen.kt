package com.filmax.feature.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filmax.core.designsystem.ShapeAsymA
import com.filmax.core.designsystem.ShapeAsymB
import com.filmax.core.designsystem.ShapeCookie
import com.filmax.core.designsystem.ShapeLg
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.ui.components.FilmaxChip
import com.filmax.core.ui.components.PosterImage
import com.filmax.core.ui.components.RatingPill

@Composable
fun DetailsScreen(
    onBack: () -> Unit,
    onPlay: (itemId: Int) -> Unit,
    onOpenItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            state.item != null -> DetailsContent(
                state    = state,
                onBack   = onBack,
                onPlay   = { onPlay(state.item!!.id) },
                onFav    = viewModel::toggleFav,
                onOpenItem = onOpenItem,
            )
        }
    }
}

@Composable
private fun DetailsContent(
    state: DetailsUiState,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onFav: () -> Unit,
    onOpenItem: (Int) -> Unit,
) {
    val item = state.item!!
    var tab by remember { mutableStateOf("about") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 120.dp),
    ) {
        // ── Hero backdrop ─────────────────────────────────────────────────────
        Box(Modifier.fillMaxWidth().height(540.dp)) {
            PosterImage(
                url = item.posters.big,
                contentDescription = item.title,
                modifier = Modifier.matchParentSize(),
                shape = RoundedCornerShape(0.dp),
                accentColor = Color(0xFFB4305A),
            )
            Box(Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    0f to Color(0xFF141012).copy(0.3f),
                    0.3f to Color.Transparent,
                    0.7f to Color.Transparent,
                    1f to MaterialTheme.colorScheme.surface,
                )
            ))
            // Top controls
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassButton(onClick = {}) { Icon(Icons.Filled.Cast, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                    GlassButton(onClick = {}) { Icon(Icons.Filled.MoreVert, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                }
            }
            // Bottom info
            Column(Modifier.align(Alignment.BottomStart).padding(20.dp).padding(bottom = 24.dp)) {
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        item.genres.firstOrNull()?.title ?: "",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer, letterSpacing = 1.5.sp,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(item.title, style = MaterialTheme.typography.headlineLarge, color = Color.White)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RatingPill(rating = item.rating.filmax / 10f)
                    Dot()
                    Text("${item.year}", fontSize = 13.sp, color = Color.White.copy(0.85f))
                    Dot()
                    Text("${item.duration.averageMinutes ?: "?"} мин", fontSize = 13.sp, color = Color.White.copy(0.85f))
                    Dot()
                    Text(item.country, fontSize = 13.sp, color = Color.White.copy(0.85f))
                }
            }
        }

        // ── Action row ────────────────────────────────────────────────────────
        Row(Modifier.padding(horizontal = 20.dp, vertical = 16.dp).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
                onClick = onPlay,
            ) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(8.dp))
                    Text("Смотреть", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            for (pair in listOf(
                Icons.Filled.Favorite to "Избранное",
                Icons.Filled.Download to "Скачать",
                Icons.Filled.Share to "Поделиться",
            )) {
                val isFavIcon = pair.second == "Избранное"
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    onClick = if (isFavIcon) onFav else { {} },
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isFavIcon && state.isFav) Icons.Filled.Favorite else if (isFavIcon) Icons.Outlined.FavoriteBorder else pair.first,
                            contentDescription = pair.second,
                            tint = if (isFavIcon && state.isFav) Color(0xFFFFB1C8) else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        // ── Tabs ─────────────────────────────────────────────────────────────
        Row(Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("about" to "О фильме", "cast" to "Актёры", "similar" to "Похожие").forEach { (id, label) ->
                FilmaxChip(label = label, selected = tab == id, onClick = { tab = id })
            }
        }

        // ── Tab content ───────────────────────────────────────────────────────
        when (tab) {
            "about" -> AboutTab(item)
            "cast"  -> CastTab(item)
            "similar" -> SimilarTab(state.similar, onOpenItem)
        }
    }
}

@Composable
private fun AboutTab(item: Item) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text(item.plot, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 20.dp))
        // Stats grid
        val stats = listOf(
            Triple(Color(0xFFB4305A), ShapeAsymA, "Рейтинг" to "${item.rating.filmax / 10f}"),
            Triple(Color(0xFFF4B792), ShapeCookie, "Длительность" to "${item.duration.averageMinutes ?: "?"} мин"),
            Triple(Color(0xFF6AC2B0), ShapeAsymB, "Режиссёр" to item.director),
            Triple(Color(0xFFE86D9E), ShapeLg, "Жанр" to (item.genres.firstOrNull()?.title ?: "—")),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.height(240.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            userScrollEnabled = false,
        ) {
            items(stats.size) { i ->
                val (color, shape, kv) = stats[i]
                StatCard(color = color, shape = shape, label = kv.first, value = kv.second)
            }
        }
    }
}

@Composable
private fun StatCard(
    color: Color,
    shape: androidx.compose.ui.graphics.Shape,
    label: String,
    value: String,
) {
    Column(
        modifier = Modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(color.copy(0.3f), color.copy(0.1f))))
            .padding(14.dp),
    ) {
        Text(label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}

@Composable
private fun CastTab(item: Item) {
    val colors = listOf(Color(0xFFB4305A), Color(0xFFE86D9E), Color(0xFFF4B792), Color(0xFF6AC2B0), Color(0xFF6B4B8F))
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text("В ролях", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 14.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            item.cast.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEachIndexed { i, name ->
                val initials = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(88.dp)) {
                    Box(
                        modifier = Modifier.size(88.dp)
                            .clip(if (i % 2 == 0) ShapeCookie else CircleShape)
                            .background(Brush.linearGradient(listOf(colors[i % colors.size], colors[(i + 1) % colors.size]))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(initials, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun SimilarTab(similarItems: List<Item>, onOpenItem: (Int) -> Unit) {
    val visible = similarItems.take(6)
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.height(320.dp).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = false,
    ) {
        items(visible) { item ->
            Box(
                modifier = Modifier
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpenItem(item.id) }
            ) {
                PosterImage(
                    url                = item.posters.medium,
                    contentDescription = item.title,
                    modifier           = Modifier.matchParentSize(),
                    accentColor        = Color(0xFFB4305A),
                )
            }
        }
    }
}

@Composable
private fun GlassButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(0x73000000))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun Dot() {
    Box(Modifier.size(3.dp).clip(CircleShape).background(Color.White.copy(0.5f)))
}

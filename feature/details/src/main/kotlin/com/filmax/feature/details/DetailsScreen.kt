package com.filmax.feature.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.androidx.compose.koinViewModel
import com.filmax.core.designsystem.ShapeAsymA
import com.filmax.core.designsystem.ShapeAsymB
import com.filmax.core.designsystem.ShapeCookie
import com.filmax.core.designsystem.ShapeLg
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.ui.components.FilmaxChip
import com.filmax.core.ui.components.FilmaxErrorModal
import com.filmax.core.ui.components.FilmaxStatCard
import com.filmax.core.ui.components.PosterImage
import com.filmax.core.ui.components.RatingPill
import java.util.Locale

private val AccentColor = Color(0xFFB4305A)

@Composable
fun DetailsScreen(
    onBack: () -> Unit,
    onPlay: (itemId: Int) -> Unit,
    onOpenItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
    screenModel: DetailsScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()
    val appError by screenModel.collectErrorAsState()
    val item = state.item

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            item != null -> DetailsContent(
                item = item,
                state = state,
                onBack = onBack,
                onPlay = { onPlay(item.id) },
                onFav = { screenModel.dispatch(DetailsEvent.ToggleFav) },
                onDownload = { screenModel.dispatch(DetailsEvent.ToggleDownload) },
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
private fun DetailsContent(
    item: Item,
    state: DetailsState,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onFav: () -> Unit,
    onDownload: () -> Unit,
) {
    var tab by remember { mutableStateOf("about") }
    val context = LocalContext.current
    val scroll = rememberScrollState()
    val density = LocalDensity.current

    // Прогресс «сворачивания» героя 0..1 на первых 360dp скролла.
    val collapseRange = with(density) { 360.dp.toPx() }
    val p = (scroll.value / collapseRange).coerceIn(0f, 1f)

    Box(Modifier.fillMaxSize()) {
        // ── Sticky hero backdrop (за контентом, с параллакс-зумом) ─────────────
        Box(
            Modifier
                .fillMaxWidth()
                .height(HeroHeight)
                .graphicsLayer {
                    scaleX = 1f + p * 0.06f
                    scaleY = 1f + p * 0.06f
                },
        ) {
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
            // Затемнение нарастает по мере сворачивания.
            Box(Modifier.matchParentSize().background(Color(0xFF0A0809).copy(alpha = p * 0.55f)))
            // Bottom info — гаснет и слегка уезжает вниз.
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .graphicsLayer {
                        alpha = (1f - p * 1.5f).coerceIn(0f, 1f)
                        translationY = p * 24.dp.toPx()
                    }
                    .padding(20.dp)
                    .padding(bottom = 24.dp),
            ) {
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
                    Text("${item.duration.averageMinutes?.toInt() ?: "?"} мин", fontSize = 13.sp, color = Color.White.copy(0.85f))
                    Dot()
                    Text(item.country, fontSize = 13.sp, color = Color.White.copy(0.85f))
                }
            }
        }

        // ── Scrolling content — наезжает на sticky hero ────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(bottom = 120.dp),
        ) {
            // Прозрачный спейсер высотой героя — сквозь него виден backdrop.
            Spacer(Modifier.height(HeroHeight))

            Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(top = 16.dp)) {
                    // ── Action row ──────────────────────────────────────────────
                    Row(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .shadow(
                                    elevation = 16.dp,
                                    shape = RoundedCornerShape(50),
                                    spotColor = AccentColor,
                                    ambientColor = AccentColor,
                                ),
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
                        ActionSquare(
                            icon = if (state.isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            desc = "Избранное",
                            tint = if (state.isFav) Color(0xFFFFB1C8) else MaterialTheme.colorScheme.onSurface,
                            onClick = onFav,
                        )
                        ActionSquare(
                            icon = if (state.isDownloaded) Icons.Filled.DownloadDone else Icons.Filled.Download,
                            desc = "Скачать",
                            tint = if (state.isDownloaded) Color(0xFF6AC2B0) else MaterialTheme.colorScheme.onSurface,
                            onClick = onDownload,
                        )
                        ActionSquare(
                            icon = Icons.Filled.Share,
                            desc = "Поделиться",
                            onClick = { shareItem(context, item) },
                        )
                    }

                    // ── Tabs ────────────────────────────────────────────────────
                    Row(Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("about" to "О фильме", "cast" to "Актёры").forEach { (id, label) ->
                            FilmaxChip(label = label, selected = tab == id, onClick = { tab = id })
                        }
                    }

                    // ── Tab content ─────────────────────────────────────────────
                    when (tab) {
                        "about" -> AboutTab(item = item, onPlayTrailer = {
                            item.trailer?.url?.let { openExternalUrl(context, it) }
                        })
                        "cast"  -> CastTab(item)
                    }
                }
            }
        }

        // ── Top glass controls — гаснут при сворачивании ───────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(12.dp)
                .graphicsLayer { alpha = (1f - p * 1.6f).coerceIn(0f, 1f) },
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

        // ── Collapsed compact app bar — проявляется при p > 0.65 ───────────────
        val barAlpha = ((p - 0.65f) / 0.35f).coerceIn(0f, 1f)
        if (barAlpha > 0f) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = barAlpha },
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private val HeroHeight = 540.dp

@Composable
private fun RowScope.ActionSquare(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(
        modifier = Modifier.size(56.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        onClick = onClick,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = desc, tint = tint)
        }
    }
}

private fun shareItem(context: android.content.Context, item: Item) {
    val text = "${item.title} (${item.year}) — смотри в Filmax"
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, text)
    }
    runCatching {
        context.startActivity(android.content.Intent.createChooser(intent, "Поделиться"))
    }
}

private fun openExternalUrl(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)),
        )
    }
}

@Composable
private fun AboutTab(item: Item, onPlayTrailer: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text(item.plot, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 20.dp))

        // Stats grid — M3 Expressive colored surfaces
        val director = item.director.ifBlank { "—" }
        val ratingValue = "%.1f".format(Locale.US, item.rating.filmax / 10f)
        val stats = listOf(
            StatItem(Color(0xFFB4305A), ShapeAsymA, "Рейтинг", ratingValue, "Filmax"),
            StatItem(Color(0xFFF4B792), ShapeCookie, "Длительность", "${item.duration.averageMinutes?.toInt() ?: "?"}", "мин"),
            StatItem(Color(0xFF6AC2B0), ShapeAsymB, "Режиссёр", director.substringBefore(" "), director.substringAfter(" ", "")),
            StatItem(Color(0xFFE86D9E), ShapeLg, "Жанр", item.genres.firstOrNull()?.title ?: "—", item.genres.getOrNull(1)?.title ?: "—"),
        )
        // Два ряда обычных Row — сетка измеряется по контенту и не накладывается на трейлер.
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            stats.chunked(2).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowItems.forEach { stat ->
                        FilmaxStatCard(
                            accent = stat.color,
                            shape = stat.shape,
                            label = stat.label,
                            value = stat.value,
                            sub = stat.sub,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // Trailer preview
        if (item.trailer != null) {
            Spacer(Modifier.height(24.dp))
            Text("Трейлер", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onPlayTrailer),
            ) {
                PosterImage(
                    url = item.posters.big,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    shape = RoundedCornerShape(0.dp),
                    accentColor = AccentColor,
                )
                Box(Modifier.matchParentSize().background(Color(0x66000000)))
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(28.dp))
                }
                Text(
                    "Официальный трейлер",
                    modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private data class StatItem(
    val color: Color,
    val shape: androidx.compose.ui.graphics.Shape,
    val label: String,
    val value: String,
    val sub: String,
)

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

        // Команда
        Spacer(Modifier.height(24.dp))
        Text("Команда", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 14.dp))
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.padding(16.dp)) {
                InfoRow("Режиссёр", item.director.ifBlank { "—" })
                InfoRow("Страна", item.country.ifBlank { "—" })
                InfoRow("Жанр", item.genres.joinToString(", ") { it.title }.ifBlank { "—" })
                InfoRow("Год", item.year.toString())
            }
        }
    }
}

@Composable
private fun InfoRow(key: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(key, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
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

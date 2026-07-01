package com.filmax.feature.details.mobile

import androidx.compose.foundation.ScrollState
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
import com.filmax.core.designsystem.ShapeAsymA
import com.filmax.core.designsystem.ShapeAsymB
import com.filmax.core.designsystem.ShapeCookie
import com.filmax.core.designsystem.ShapeLg
import com.filmax.core.ui.components.BackdropGradients
import com.filmax.core.ui.components.FilmaxChip
import com.filmax.core.ui.components.FilmaxErrorModal
import com.filmax.core.ui.components.FilmaxStatCard
import com.filmax.core.ui.components.HeroBackdrop
import com.filmax.core.ui.components.PosterImage
import com.filmax.core.ui.components.RatingPill
import com.filmax.feature.details.common.DetailsEvent
import com.filmax.feature.details.common.DetailsScreenModel
import com.filmax.feature.details.common.DetailsTab
import com.filmax.feature.details.common.DetailsUi
import com.filmax.feature.details.common.DetailsUiState
import org.koin.androidx.compose.koinViewModel

private val AccentColor = Color(0xFFB4305A)

@Composable
fun DetailsScreen(
    onBack: () -> Unit,
    onPlay: (itemId: Int) -> Unit,
    // Зарезервировано под навигацию к похожим из деталей (как на TV); ядро mobile пока не вызывает.
    @Suppress("UnusedParameter") onOpenItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
    screenModel: DetailsScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()
    val appError by screenModel.collectErrorAsState()
    val details = state.details

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            details != null -> DetailsContent(
                details = details,
                state = state,
                actions = DetailsActions(
                    onBack = onBack,
                    onPlay = { onPlay(details.id) },
                    onFav = { screenModel.dispatch(DetailsEvent.ToggleFav) },
                    onDownload = { screenModel.dispatch(DetailsEvent.ToggleDownload) },
                    onSelectTab = { screenModel.dispatch(DetailsEvent.SelectTab(it)) },
                ),
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

private data class DetailsActions(
    val onBack: () -> Unit,
    val onPlay: () -> Unit,
    val onFav: () -> Unit,
    val onDownload: () -> Unit,
    val onSelectTab: (DetailsTab) -> Unit,
)

@Composable
private fun DetailsContent(
    details: DetailsUi,
    state: DetailsUiState,
    actions: DetailsActions,
) {
    val scroll = rememberScrollState()
    val density = LocalDensity.current

    // Прогресс «сворачивания» героя 0..1 на первых 360dp скролла.
    val collapseRange = with(density) { 360.dp.toPx() }
    val collapseProgress = (scroll.value / collapseRange).coerceIn(0f, 1f)

    Box(Modifier.fillMaxSize()) {
        // ── Sticky hero backdrop (за контентом, с параллакс-зумом) ─────────────
        DetailsHero(details = details, collapseProgress = collapseProgress)
        // ── Scrolling content — наезжает на sticky hero ────────────────────────
        DetailsScrollContent(details = details, state = state, scroll = scroll, actions = actions)
        // ── Top glass controls — гаснут при сворачивании ───────────────────────
        DetailsTopControls(onBack = actions.onBack, collapseProgress = collapseProgress)
        // ── Collapsed compact app bar — проявляется при p > 0.65 ───────────────
        DetailsCollapsedBar(title = details.title, collapseProgress = collapseProgress, onBack = actions.onBack)
    }
}

@Composable
private fun DetailsHero(details: DetailsUi, collapseProgress: Float) {
    val surface = MaterialTheme.colorScheme.surface
    Box(
        Modifier
            .fillMaxWidth()
            .height(HeroHeight)
            .graphicsLayer {
                scaleX = 1f + collapseProgress * 0.06f
                scaleY = 1f + collapseProgress * 0.06f
            },
    ) {
        // Общий постер + базовый вертикальный градиент (см. core:ui HeroBackdrop).
        HeroBackdrop(
            posterUrl = details.posterBig,
            contentDescription = details.title,
            scrims = listOf(BackdropGradients.mobileVertical(surface)),
            modifier = Modifier.matchParentSize(),
            accentColor = AccentColor,
        )
        // Затемнение нарастает по мере сворачивания.
        Box(Modifier.matchParentSize().background(BackdropGradients.collapseScrim(collapseProgress)))
        // Bottom info — гаснет и слегка уезжает вниз.
        DetailsHeroInfo(
            details = details,
            collapseProgress = collapseProgress,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

@Composable
private fun DetailsHeroInfo(details: DetailsUi, collapseProgress: Float, modifier: Modifier = Modifier) {
    Column(
        modifier
            .graphicsLayer {
                alpha = (1f - collapseProgress * 1.5f).coerceIn(0f, 1f)
                translationY = collapseProgress * 24.dp.toPx()
            }
            .padding(20.dp)
            .padding(bottom = 24.dp),
    ) {
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
            Text(
                details.primaryGenre,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                letterSpacing = 1.5.sp,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(details.title, style = MaterialTheme.typography.headlineLarge, color = Color.White)
        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RatingPill(rating = details.externalRating)
            Dot()
            Text("${details.year}", fontSize = 13.sp, color = Color.White.copy(0.85f))
            Dot()
            Text(
                "${details.durationMinutes ?: "?"} мин",
                fontSize = 13.sp,
                color = Color.White.copy(0.85f)
            )
            Dot()
            Text(details.country, fontSize = 13.sp, color = Color.White.copy(0.85f))
        }
    }
}

@Composable
private fun DetailsScrollContent(
    details: DetailsUi,
    state: DetailsUiState,
    scroll: ScrollState,
    actions: DetailsActions,
) {
    val context = LocalContext.current
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
                DetailsActionRow(details = details, state = state, actions = actions, context = context)

                // ── Tabs ────────────────────────────────────────────────────
                DetailsTabRow(selectedTab = state.selectedTab, onTabChange = actions.onSelectTab)

                // ── Tab content ─────────────────────────────────────────────
                when (state.selectedTab) {
                    DetailsTab.ABOUT -> AboutTab(details = details, onPlayTrailer = {
                        details.trailerUrl?.let { openExternalUrl(context, it) }
                    })
                    DetailsTab.CAST -> CastTab(details)
                }
            }
        }
    }
}

@Composable
private fun DetailsActionRow(
    details: DetailsUi,
    state: DetailsUiState,
    actions: DetailsActions,
    context: android.content.Context,
) {
    Row(
        Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
            onClick = actions.onPlay,
        ) {
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Смотреть",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        ActionSquare(
            icon = if (state.isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            desc = "Избранное",
            tint = if (state.isFav) Color(0xFFFFB1C8) else MaterialTheme.colorScheme.onSurface,
            onClick = actions.onFav,
        )
        ActionSquare(
            icon = if (state.isDownloaded) Icons.Filled.DownloadDone else Icons.Filled.Download,
            desc = "Скачать",
            tint = if (state.isDownloaded) Color(0xFF6AC2B0) else MaterialTheme.colorScheme.onSurface,
            onClick = actions.onDownload,
        )
        ActionSquare(
            icon = Icons.Filled.Share,
            desc = "Поделиться",
            onClick = { shareItem(context, details) },
        )
    }
}

@Composable
private fun DetailsTabRow(selectedTab: DetailsTab, onTabChange: (DetailsTab) -> Unit) {
    Row(
        Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(DetailsTab.ABOUT to "О фильме", DetailsTab.CAST to "Актёры").forEach { (tab, label) ->
            FilmaxChip(label = label, selected = selectedTab == tab, onClick = { onTabChange(tab) })
        }
    }
}

@Composable
private fun DetailsTopControls(onBack: () -> Unit, collapseProgress: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(12.dp)
            .graphicsLayer { alpha = (1f - collapseProgress * 1.6f).coerceIn(0f, 1f) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        GlassButton(onClick = {
        }) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun DetailsCollapsedBar(title: String, collapseProgress: Float, onBack: () -> Unit) {
    val barAlpha = ((collapseProgress - 0.65f) / 0.35f).coerceIn(0f, 1f)
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
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    title,
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

private fun shareItem(context: android.content.Context, details: DetailsUi) {
    val text = "${details.title} (${details.year}) — смотри в Filmax"
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
private fun AboutTab(details: DetailsUi, onPlayTrailer: () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text(
            details.plot,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        AboutStatsGrid(details = details)

        // Trailer preview
        if (details.trailerUrl != null) {
            AboutTrailer(details = details, onPlayTrailer = onPlayTrailer)
        }
    }
}

/** Акцент и форма каждой из четырёх stat-карточек; содержимое приходит готовым из стейта. */
private val StatStyles = listOf(
    Color(0xFFB4305A) to ShapeAsymA,
    Color(0xFFF4B792) to ShapeCookie,
    Color(0xFF6AC2B0) to ShapeAsymB,
    Color(0xFFE86D9E) to ShapeLg,
)

@Composable
private fun AboutStatsGrid(details: DetailsUi) {
    // Два ряда обычных Row — сетка измеряется по контенту и не накладывается на трейлер.
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        details.stats.zip(StatStyles).chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEach { (stat, style) ->
                    FilmaxStatCard(
                        accent = style.first,
                        shape = style.second,
                        label = stat.label,
                        value = stat.value,
                        sub = stat.sub,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutTrailer(details: DetailsUi, onPlayTrailer: () -> Unit) {
    Spacer(Modifier.height(24.dp))
    Text(
        "Трейлер",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 12.dp)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onPlayTrailer),
    ) {
        PosterImage(
            url = details.posterBig,
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
            Icon(
                Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
            )
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

@Composable
private fun CastTab(details: DetailsUi) {
    val colors = listOf(Color(0xFFB4305A), Color(0xFFE86D9E), Color(0xFFF4B792), Color(0xFF6AC2B0), Color(0xFF6B4B8F))
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text(
            "В ролях",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 14.dp)
        )
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            details.castMembers.forEachIndexed { index, member ->
                CastMemberCell(
                    name = member.name,
                    initials = member.initials,
                    shape = if (index % 2 == 0) ShapeCookie else CircleShape,
                    gradient = Brush.linearGradient(
                        listOf(colors[index % colors.size], colors[(index + 1) % colors.size]),
                    ),
                )
            }
        }

        // Команда
        Spacer(Modifier.height(24.dp))
        Text(
            "Команда",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 14.dp)
        )
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.padding(16.dp)) {
                details.crew.forEach { row -> InfoRow(row.label, row.value) }
            }
        }
    }
}

@Composable
private fun CastMemberCell(
    name: String,
    initials: String,
    shape: androidx.compose.ui.graphics.Shape,
    gradient: Brush,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(88.dp)) {
        Box(
            modifier = Modifier.size(88.dp)
                .clip(shape)
                .background(gradient),
            contentAlignment = Alignment.Center,
        ) {
            Text(initials, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            name,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
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

package com.filmax.feature.details.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.tv.designsystem.TvButton
import com.filmax.core.tv.designsystem.TvFocus
import com.filmax.core.tv.designsystem.TvFocusCard
import com.filmax.core.ui.components.PosterImage
import com.filmax.feature.details.common.DetailsEvent
import com.filmax.feature.details.common.DetailsScreenModel
import org.koin.androidx.compose.koinViewModel

private val Accent = Color(0xFFB4305A)
private val AccentDark = Color(0xFF5E1133)
private val GlassPanel = Color(0xB3141012)
private val GlassStrip = Color(0xA8141012)
private val ChipFill = Color(0x1AFFFFFF)
private val ChipBorder = Color(0x26FFFFFF)

/** Тёмный плотный фон неактивных чипов сезонов — чтобы не терялись на светлом бэкдропе. */
private val SeasonChipInactive = Color(0xD91A1518)

/** Сериалоподобный контент: список эпизодов (больше одного трека) или тип «сериал»/«докусериал». */
private fun Item.isSeriesLike(): Boolean =
    tracklist.size > 1 || type == ItemType.SERIES || type == ItemType.DOCUMENTARY

/**
 * TV-Детали (экран 06 макета). Для фильма — бэкдроп + действия + glass-панель «в ролях/качество»
 * + рельса «похожие». Для сериала — браузер сезонов и серий с продолжением просмотра.
 * Поверх общего [DetailsScreenModel] (itemId берётся из маршрута через SavedStateHandle).
 */
@Composable
fun TvDetailsScreen(
    onPlay: (itemId: Int, videoId: Int) -> Unit,
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

            state.item != null -> {
                val item = state.item!!
                if (item.isSeriesLike()) {
                    SeriesContent(
                        item = item,
                        isFav = state.isFav,
                        onPlayEpisode = { videoId -> onPlay(item.id, videoId) },
                        onToggleFav = { screenModel.dispatch(DetailsEvent.ToggleFav) },
                    )
                } else {
                    MovieContent(
                        item = item,
                        similar = state.similar,
                        isFav = state.isFav,
                        onPlay = { onPlay(item.id, -1) },
                        onToggleFav = { screenModel.dispatch(DetailsEvent.ToggleFav) },
                        onOpenItem = onOpenItem,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────── Общие части ───────────────────────────────

/** Бэкдроп + горизонтальный и вертикальный градиенты затемнения (как в макете). */
@Composable
private fun BoxScope.Backdrop(item: Item) {
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
}

@Composable
private fun TagPill(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(Accent)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(text.uppercase(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
    }
}

/** Строка метаданных: рейтинг-пилюля (если есть) + год · [длительность] · страна · жанры. */
@Composable
private fun MetaRow(item: Item, includeDuration: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        item.rating.external?.let { rating ->
            Box(
                Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color(0x8C000000))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = if (rating >= 8.5) Color(0xFF6AC2B0) else Color(0xFFE8A43A), modifier = Modifier.size(16.dp))
                    Text(String.format("%.1f", rating), color = if (rating >= 8.5) Color(0xFF6AC2B0) else Color(0xFFE8A43A), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(
            buildString {
                append(item.year)
                if (includeDuration) {
                    item.duration.averageMinutes?.toInt()?.takeIf { it > 0 }?.let { append("  ·  ${it / 60}ч ${it % 60}м") }
                }
                if (item.country.isNotBlank()) append("  ·  ${item.country}")
                if (item.genres.isNotEmpty()) append("  ·  ${item.genres.take(2).joinToString(" · ") { it.title }}")
            },
            fontSize = 16.sp, color = Color.White.copy(alpha = 0.9f),
        )
    }
}

@Composable
private fun Label(text: String, color: Color = Accent) {
    Text(text.uppercase(), color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, modifier = Modifier.padding(bottom = 8.dp))
}

// ─────────────────────────────── Детали ФИЛЬМА ──────────────────────────────

@Composable
private fun MovieContent(
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
        Backdrop(item)

        Row(Modifier.fillMaxSize().padding(72.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                TagPill(item.genres.take(2).joinToString(" · ") { it.title })
                Spacer(Modifier.height(16.dp))
                Text(item.title, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Spacer(Modifier.height(14.dp))
                MetaRow(item, includeDuration = true)
                Spacer(Modifier.height(16.dp))
                Text(item.plot, fontSize = 18.sp, lineHeight = 26.sp, color = Color.White.copy(alpha = 0.85f), maxLines = 4, modifier = Modifier.fillMaxWidth(0.9f))
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
            }

            // Правая glass-панель — «В ролях» / «Режиссёр» / «Качество» (как в макете TVDetails).
            if (item.cast.isNotBlank() || item.director.isNotBlank() || item.tracklist.isNotEmpty()) {
                Spacer(Modifier.width(40.dp))
                InfoPanel(item = item, modifier = Modifier.width(340.dp).align(Alignment.Top))
            }
        }

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

/**
 * Правая glass-панель деталей фильма с асимметричным скруглением 48/24/48/24 (как в макете).
 * «В ролях» ограничено по высоте, чтобы все три секции (роли/режиссёр/качество) помещались и
 * ничего не срезалось краем карточки. Если состав не влез — панель фокусируема и по нажатию
 * раскрывает полный список в оверлее [InfoPanelDialog].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InfoPanel(item: Item, modifier: Modifier = Modifier) {
    // В свёрнутой карточке — только видео-качества (помещаются в ряд); аудио-языки и полный
    // список уезжают в оверлей, чтобы чипы не переполняли узкую панель по ширине.
    val videoQualities = remember(item) {
        item.tracklist.flatMap { it.files }.map { it.quality }.filter { it.isNotBlank() }.distinct().take(5)
    }
    val fullQuality = remember(item) {
        val langs = item.tracklist.flatMap { it.audios }.mapNotNull { it.lang }.map { audioLabel(it) }.distinct()
        (videoQualities + langs).distinct().take(8)
    }
    var expanded by remember(item.id) { mutableStateOf(false) }
    // Состав реально не поместился в отведённые строки — тогда панель кликабельна и раскрывается.
    var castOverflow by remember(item.id) { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }

    val shape = RoundedCornerShape(topStart = 48.dp, topEnd = 24.dp, bottomEnd = 48.dp, bottomStart = 24.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(GlassPanel)
            // clickable сам по себе фокусируемый и реагирует на DPAD_CENTER — отдельный focusable
            // не нужен (он перехватил бы фокус, и центр не доходил бы до клика).
            .then(
                if (castOverflow) {
                    Modifier
                        .onFocusChanged { focused = it.isFocused }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { expanded = true }
                } else {
                    Modifier
                }
            )
            .then(if (focused) Modifier.border(3.dp, TvFocus, shape) else Modifier)
            .padding(24.dp),
    ) {
        if (item.cast.isNotBlank()) {
            Label("В ролях")
            Text(
                item.cast,
                fontSize = 16.sp,
                lineHeight = 26.sp,
                color = Color.White.copy(alpha = 0.92f),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { castOverflow = it.hasVisualOverflow },
            )
        }
        if (item.director.isNotBlank()) {
            Spacer(Modifier.height(18.dp))
            Label("Режиссёр")
            Text(item.director, fontSize = 16.sp, color = Color.White.copy(alpha = 0.9f))
        }
        if (videoQualities.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Label("Качество")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                videoQualities.forEach { QualityChip(it) }
            }
        }
        if (castOverflow) {
            Spacer(Modifier.height(12.dp))
            Text("Подробнее →", color = Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (expanded) {
        InfoPanelDialog(item = item, quality = fullQuality, onDismiss = { expanded = false })
    }
}

/** Оверлей с полным составом/режиссёром/качеством; «Назад» закрывает (onDismissRequest). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InfoPanelDialog(item: Item, quality: List<String>, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .widthIn(max = 720.dp)
                .heightIn(max = 640.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xF21A1518))
                .verticalScroll(rememberScrollState())
                .focusable()
                .padding(36.dp),
        ) {
            Text(item.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.White)
            if (item.cast.isNotBlank()) {
                Spacer(Modifier.height(20.dp))
                Label("В ролях")
                Text(item.cast, fontSize = 17.sp, lineHeight = 28.sp, color = Color.White.copy(alpha = 0.92f))
            }
            if (item.director.isNotBlank()) {
                Spacer(Modifier.height(20.dp))
                Label("Режиссёр")
                Text(item.director, fontSize = 17.sp, color = Color.White.copy(alpha = 0.9f))
            }
            if (quality.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Label("Качество")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    quality.forEach { QualityChip(it) }
                }
            }
        }
    }
}

@Composable
private fun QualityChip(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(ChipFill)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
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

// ─────────────────────────────── Детали СЕРИАЛА ─────────────────────────────

@Composable
private fun SeriesContent(
    item: Item,
    isFav: Boolean,
    onPlayEpisode: (videoId: Int) -> Unit,
    onToggleFav: () -> Unit,
) {
    // Сезоны: группируем эпизоды по номеру сезона, сохраняя порядок серий.
    val seasons = remember(item) {
        item.tracklist
            .groupBy { it.seasonNumber }
            .toSortedMap()
            .map { (number, episodes) -> number to episodes.sortedBy { it.number } }
    }
    // Продолжить: эпизод «в процессе», иначе последний досмотренный, иначе первый.
    val resume = remember(item) {
        item.tracklist.firstOrNull { it.watchStatus == 0 }
            ?: item.tracklist.lastOrNull { it.watchStatus == 1 }
    }
    val resumeSeasonIndex = remember(item) {
        resume?.let { r -> seasons.indexOfFirst { it.first == r.seasonNumber }.takeIf { it >= 0 } } ?: 0
    }
    var selectedSeason by remember(item.id) { mutableIntStateOf(resumeSeasonIndex) }
    val currentEpisodes = seasons.getOrNull(selectedSeason)?.second ?: emptyList()

    val playFocus = remember { FocusRequester() }
    LaunchedEffect(item.id) { runCatching { playFocus.requestFocus() } }

    Box(Modifier.fillMaxSize()) {
        Backdrop(item)

        Row(Modifier.fillMaxSize().padding(start = 72.dp, top = 56.dp, end = 72.dp, bottom = 40.dp)) {
            // ── Левая колонка: инфо + продолжить + действия ──
            Column(modifier = Modifier.weight(1f).padding(end = 20.dp)) {
                TagPill("Сериал · ${seasons.size} ${seasonsWord(seasons.size)}")
                Spacer(Modifier.height(16.dp))
                Text(item.title, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 2)
                Spacer(Modifier.height(14.dp))
                MetaRow(item, includeDuration = false)
                Spacer(Modifier.height(16.dp))
                Text(item.plot, fontSize = 17.sp, lineHeight = 25.sp, color = Color.White.copy(alpha = 0.85f), maxLines = 4)

                if (resume != null && resume.durationSeconds > 0) {
                    Spacer(Modifier.height(22.dp))
                    ResumeStrip(episode = resume)
                }

                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val first = currentEpisodes.firstOrNull() ?: item.tracklist.firstOrNull()
                    val target = resume ?: first
                    TvButton(
                        text = if (resume != null) "Продолжить" else "Смотреть",
                        onClick = { target?.let { onPlayEpisode(it.id) } },
                        leadingIcon = Icons.Filled.PlayArrow,
                        focusRequester = playFocus,
                    )
                    TvButton(
                        text = if (isFav) "В избранном" else "В избранное",
                        onClick = onToggleFav,
                        primary = false,
                        leadingIcon = if (isFav) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    )
                }
            }

            // ── Правая колонка: чипы сезонов + список серий ──
            Column(modifier = Modifier.width(380.dp)) {
                if (seasons.size > 1) {
                    SeasonChips(
                        seasons = seasons,
                        selectedSeason = selectedSeason,
                        onSelect = { selectedSeason = it },
                    )
                    Spacer(Modifier.height(16.dp))
                }
                Label("${currentEpisodes.size} ${episodesWord(currentEpisodes.size)}", color = Color.White.copy(alpha = 0.7f))
                Spacer(Modifier.height(4.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(currentEpisodes, key = { it.id }) { episode ->
                        EpisodeRow(
                            episode = episode,
                            isResume = episode.id == resume?.id,
                            onClick = { onPlayEpisode(episode.id) },
                        )
                    }
                }
            }
        }
    }
}

/** Полоска «Вы остановились» с миниатюрой эпизода и прогрессом. */
@Composable
private fun ResumeStrip(episode: MediaTrack) {
    val progress = (episode.watchedSeconds.toFloat() / episode.durationSeconds).coerceIn(0f, 1f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(GlassStrip)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 96.dp, height = 56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Accent, AccentDark))),
            contentAlignment = Alignment.Center,
        ) {
            Text("${episode.number}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
        Column(modifier = Modifier.weight(1f)) {
            Label("Вы остановились")
            Text("Сезон ${episode.seasonNumber} · Серия ${episode.number}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color.White.copy(alpha = 0.2f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .height(6.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Accent),
                )
            }
        }
    }
}

/**
 * Чипы сезонов переносятся на новую строку (как `flexWrap: 'wrap'` в макете), а не
 * скроллятся горизонтально — иначе крайний чип режется посреди слова у границы колонки.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeasonChips(
    seasons: List<Pair<Int, List<MediaTrack>>>,
    selectedSeason: Int,
    onSelect: (Int) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        seasons.forEachIndexed { index, (number, _) ->
            SeasonChip(
                label = if (number > 0) "Сезон $number" else "Серии",
                active = index == selectedSeason,
                onClick = { onSelect(index) },
            )
        }
    }
}

@Composable
private fun SeasonChip(label: String, active: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(percent = 50)
    TvFocusCard(onClick = onClick, shape = shape) {
        Box(
            Modifier
                .clip(shape)
                .background(if (active) Accent else SeasonChipInactive)
                .padding(horizontal = 22.dp, vertical = 11.dp),
        ) {
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun EpisodeRow(episode: MediaTrack, isResume: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    TvFocusCard(onClick = onClick, shape = shape, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(GlassStrip)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 100.dp, height = 58.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(Accent, AccentDark))),
                contentAlignment = Alignment.Center,
            ) {
                if (episode.thumbnail.isNotBlank()) {
                    PosterImage(
                        url = episode.thumbnail,
                        contentDescription = episode.title,
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(12.dp),
                        accentColor = Accent,
                    )
                } else {
                    Text("${episode.number}", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    buildString {
                        append("Серия ${episode.number}")
                        if (episode.title.isNotBlank()) append(". ${episode.title}")
                    },
                    fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    buildString {
                        episode.durationSeconds.takeIf { it > 0 }?.let { append("${it / 60} мин") }
                        if (isResume) append(if (length > 0) " · продолжить" else "продолжить")
                    },
                    fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f),
                )
            }
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

private fun seasonsWord(count: Int): String = when {
    count % 100 in 11..14 -> "сезонов"
    count % 10 == 1 -> "сезон"
    count % 10 in 2..4 -> "сезона"
    else -> "сезонов"
}

private fun episodesWord(count: Int): String = when {
    count % 100 in 11..14 -> "серий"
    count % 10 == 1 -> "серия"
    count % 10 in 2..4 -> "серии"
    else -> "серий"
}

private fun audioLabel(code: String): String = when (code.lowercase()) {
    "rus", "ru" -> "Русский"
    "eng", "en" -> "Eng"
    "ukr", "uk" -> "Укр"
    else -> code
}

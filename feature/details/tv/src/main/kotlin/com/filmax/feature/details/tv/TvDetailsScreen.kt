package com.filmax.feature.details.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.filmax.core.tv.designsystem.TvButton
import com.filmax.core.tv.designsystem.TvFocus
import com.filmax.core.tv.designsystem.TvFocusCard
import com.filmax.core.ui.components.BackdropGradients
import com.filmax.core.ui.components.HeroBackdrop
import com.filmax.core.ui.components.PosterImage
import com.filmax.feature.details.common.DetailsEvent
import com.filmax.feature.details.common.DetailsScreenModel
import com.filmax.feature.details.common.DetailsUi
import com.filmax.feature.details.common.DetailsUiState
import com.filmax.feature.details.common.EpisodeUi
import com.filmax.feature.details.common.ResumeUi
import com.filmax.feature.details.common.SeasonUi
import com.filmax.feature.details.common.SimilarUi
import org.koin.androidx.compose.koinViewModel

private val Accent = Color(0xFFB4305A)
private val AccentDark = Color(0xFF5E1133)
private val GlassPanel = Color(0xB3141012)
private val GlassStrip = Color(0xA8141012)
private val ChipFill = Color(0x1AFFFFFF)

/** Тёмный плотный фон неактивных чипов сезонов — чтобы не терялись на светлом бэкдропе. */
private val SeasonChipInactive = Color(0xD91A1518)

/**
 * TV-Детали (экран 06 макета). Для фильма — бэкдроп + действия + glass-панель «в ролях/качество»
 * + рельса «похожие». Для сериала — браузер сезонов и серий с продолжением просмотра.
 * Поверх общего [DetailsScreenModel] (itemId берётся из маршрута через SavedStateHandle);
 * все данные приходят готовыми в [DetailsUi] — экран только отрисовывает.
 */
@Composable
fun TvDetailsScreen(
    onPlay: (itemId: Int, videoId: Int) -> Unit,
    onOpenItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
    screenModel: DetailsScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()
    val details = state.details

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

            details != null -> {
                if (details.isSeriesLike) {
                    SeriesContent(
                        details = details,
                        state = state,
                        actions = SeriesActions(
                            onPlayEpisode = { videoId -> onPlay(details.id, videoId) },
                            onToggleFav = { screenModel.dispatch(DetailsEvent.ToggleFav) },
                            onSelectSeason = { screenModel.dispatch(DetailsEvent.SelectSeason(it)) },
                        ),
                    )
                } else {
                    MovieContent(
                        details = details,
                        isFav = state.isFav,
                        actions = MovieActions(
                            onPlay = { onPlay(details.id, -1) },
                            onToggleFav = { screenModel.dispatch(DetailsEvent.ToggleFav) },
                            onOpenItem = onOpenItem,
                        ),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────── Общие части ───────────────────────────────

/** Бэкдроп + горизонтальный и вертикальный градиенты затемнения (как в макете). */
@Composable
private fun Backdrop(details: DetailsUi) {
    val surface = MaterialTheme.colorScheme.surface
    HeroBackdrop(
        posterUrl = details.backdropUrl,
        contentDescription = details.title,
        scrims = listOf(
            BackdropGradients.tvHorizontal(surface),
            BackdropGradients.tvVerticalBottom(surface),
        ),
        modifier = Modifier.fillMaxSize(),
        accentColor = Accent,
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
        Text(
            text.uppercase(),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )
    }
}

/** Строка метаданных: рейтинг-пилюля (если есть оценка) + готовая строка меты из стейта. */
@Composable
private fun MetaRow(details: DetailsUi, line: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        if (details.externalRating != null) {
            RatingBadge(text = details.externalRatingText, isHigh = details.isRatingHigh)
        }
        Text(
            line,
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.9f),
        )
    }
}

@Composable
private fun RatingBadge(text: String, isHigh: Boolean) {
    val color = if (isHigh) Color(0xFF6AC2B0) else Color(0xFFE8A43A)
    Box(
        Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(Color(0x8C000000))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text,
                color = color,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun Label(text: String, color: Color = Accent) {
    Text(
        text.uppercase(),
        color = color,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

// ─────────────────────────────── Детали ФИЛЬМА ──────────────────────────────

/** Действия деталей фильма — сгруппированы, чтобы не раздувать список параметров. */
private data class MovieActions(
    val onPlay: () -> Unit,
    val onToggleFav: () -> Unit,
    val onOpenItem: (Int) -> Unit,
)

@Composable
private fun MovieContent(
    details: DetailsUi,
    isFav: Boolean,
    actions: MovieActions,
) {
    Box(Modifier.fillMaxSize()) {
        Backdrop(details)

        // Корневой вертикальный скролл: hero-блок и рельса «Похожие» — отдельные элементы потока,
        // а не наложенные оверлеи. Раньше «Похожие» висели поверх кнопок (align(BottomStart) в
        // статичном Box без скролла) и перекрывали их без возможности докрутить. LazyColumn при
        // уходе фокуса вниз сам подтягивает скрытый элемент в видимую область (bring-into-view).
        // Backdrop остаётся зафиксированным фоном за списком — как sticky-бэкдроп на телефоне.
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 48.dp),
        ) {
            item(key = "hero") {
                MovieHero(
                    details = details,
                    isFav = isFav,
                    onPlay = actions.onPlay,
                    onToggleFav = actions.onToggleFav,
                )
            }

            if (details.similar.isNotEmpty()) {
                item(key = "similar") {
                    MovieSimilar(similar = details.similar, onOpenItem = actions.onOpenItem)
                }
            }
        }
    }
}

@Composable
private fun MovieHero(
    details: DetailsUi,
    isFav: Boolean,
    onPlay: () -> Unit,
    onToggleFav: () -> Unit,
) {
    val playFocus = remember { FocusRequester() }
    LaunchedEffect(details.id) { runCatching { playFocus.requestFocus() } }

    Row(Modifier.fillMaxWidth().padding(72.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            TagPill(details.topGenres)
            Spacer(Modifier.height(16.dp))
            Text(
                details.title,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(Modifier.height(14.dp))
            MetaRow(details, line = details.metaLine)
            Spacer(Modifier.height(16.dp))
            Text(
                details.plot,
                fontSize = 18.sp,
                lineHeight = 26.sp,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(0.9f)
            )
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TvButton(
                    "Смотреть",
                    onClick = onPlay,
                    leadingIcon = Icons.Filled.PlayArrow,
                    focusRequester = playFocus
                )
                TvButton(
                    text = if (isFav) "В избранном" else "В избранное",
                    onClick = onToggleFav,
                    primary = false,
                    leadingIcon = if (isFav) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                )
            }
        }

        // Правая glass-панель — «В ролях» / «Режиссёр» / «Качество» (как в макете TVDetails).
        if (details.castLine.isNotBlank() || details.director.isNotBlank() || details.videoQualities.isNotEmpty()) {
            Spacer(Modifier.width(40.dp))
            InfoPanel(details = details, modifier = Modifier.width(340.dp).align(Alignment.Top))
        }
    }
}

@Composable
private fun MovieSimilar(similar: List<SimilarUi>, onOpenItem: (Int) -> Unit) {
    Column(Modifier.padding(start = 72.dp, bottom = 32.dp)) {
        Text(
            "Похожие",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 14.dp)
        )
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

/**
 * Правая glass-панель деталей фильма с асимметричным скруглением 48/24/48/24 (как в макете).
 * «В ролях» ограничено по высоте, чтобы все три секции (роли/режиссёр/качество) помещались и
 * ничего не срезалось краем карточки. Если состав не влез — панель фокусируема и по нажатию
 * раскрывает полный список в оверлее [InfoPanelDialog].
 */
@Composable
private fun InfoPanel(details: DetailsUi, modifier: Modifier = Modifier) {
    var expanded by remember(details.id) { mutableStateOf(false) }
    // Состав реально не поместился в отведённые строки — тогда панель кликабельна и раскрывается.
    var castOverflow by remember(details.id) { mutableStateOf(false) }
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
        InfoPanelSections(
            details = details,
            castOverflow = castOverflow,
            onCastOverflowChange = { castOverflow = it },
        )
    }

    if (expanded) {
        InfoPanelDialog(details = details, onDismiss = { expanded = false })
    }
}

/** Секции свёрнутой glass-панели (роли/режиссёр/качество). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InfoPanelSections(
    details: DetailsUi,
    castOverflow: Boolean,
    onCastOverflowChange: (Boolean) -> Unit,
) {
    if (details.castLine.isNotBlank()) {
        Label("В ролях")
        Text(
            details.castLine,
            fontSize = 16.sp,
            lineHeight = 26.sp,
            color = Color.White.copy(alpha = 0.92f),
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { onCastOverflowChange(it.hasVisualOverflow) },
        )
    }
    if (details.director.isNotBlank()) {
        Spacer(Modifier.height(18.dp))
        Label("Режиссёр")
        Text(details.director, fontSize = 16.sp, color = Color.White.copy(alpha = 0.9f))
    }
    if (details.videoQualities.isNotEmpty()) {
        Spacer(Modifier.height(18.dp))
        Label("Качество")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            details.videoQualities.forEach { QualityChip(it) }
        }
    }
    if (castOverflow) {
        Spacer(Modifier.height(12.dp))
        Text("Подробнее →", color = Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

/** Оверлей с полным составом/режиссёром/качеством; «Назад» закрывает (onDismissRequest). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InfoPanelDialog(details: DetailsUi, onDismiss: () -> Unit) {
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
            Text(
                details.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            if (details.castLine.isNotBlank()) {
                Spacer(Modifier.height(20.dp))
                Label("В ролях")
                Text(details.castLine, fontSize = 17.sp, lineHeight = 28.sp, color = Color.White.copy(alpha = 0.92f))
            }
            if (details.director.isNotBlank()) {
                Spacer(Modifier.height(20.dp))
                Label("Режиссёр")
                Text(details.director, fontSize = 17.sp, color = Color.White.copy(alpha = 0.9f))
            }
            if (details.qualityBadges.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Label("Качество")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    details.qualityBadges.forEach { QualityChip(it) }
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
private fun SimilarCard(item: SimilarUi, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    TvFocusCard(onClick = onClick, shape = shape, modifier = Modifier.size(width = 150.dp, height = 212.dp)) {
        PosterImage(
            url = item.posterUrl,
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            shape = shape,
            accentColor = Accent,
        )
    }
}

// ─────────────────────────────── Детали СЕРИАЛА ─────────────────────────────

/** Действия браузера сериала — сгруппированы, чтобы не раздувать список параметров. */
private data class SeriesActions(
    val onPlayEpisode: (videoId: Int) -> Unit,
    val onToggleFav: () -> Unit,
    val onSelectSeason: (Int) -> Unit,
)

/** Кнопки «Продолжить/Смотреть» + фокус — сгруппированы, чтобы не раздувать список параметров. */
private data class SeriesPlaybackActions(
    val playFocus: FocusRequester,
    val onPlay: () -> Unit,
    val onToggleFav: () -> Unit,
)

@Composable
private fun SeriesContent(
    details: DetailsUi,
    state: DetailsUiState,
    actions: SeriesActions,
) {
    val playFocus = remember { FocusRequester() }
    LaunchedEffect(details.id) { runCatching { playFocus.requestFocus() } }

    // Снимаем значение до лямбды клика: playEpisodeId вычисляется от выбранного сезона.
    val playEpisodeId = state.playEpisodeId

    Box(Modifier.fillMaxSize()) {
        Backdrop(details)

        Row(Modifier.fillMaxSize().padding(start = 72.dp, top = 56.dp, end = 72.dp, bottom = 40.dp)) {
            // ── Левая колонка: инфо + продолжить + действия ──
            SeriesInfoColumn(
                details = details,
                isFav = state.isFav,
                actions = SeriesPlaybackActions(
                    playFocus = playFocus,
                    onPlay = { playEpisodeId?.let(actions.onPlayEpisode) },
                    onToggleFav = actions.onToggleFav,
                ),
            )

            // ── Правая колонка: чипы сезонов + список серий ──
            SeriesEpisodesColumn(
                seasons = details.seasons,
                selectedSeasonIndex = state.selectedSeasonIndex,
                currentSeason = state.currentSeason,
                onSelectSeason = actions.onSelectSeason,
                onPlayEpisode = actions.onPlayEpisode,
            )
        }
    }
}

@Composable
private fun RowScope.SeriesInfoColumn(
    details: DetailsUi,
    isFav: Boolean,
    actions: SeriesPlaybackActions,
) {
    Column(modifier = Modifier.weight(1f).padding(end = 20.dp)) {
        TagPill(details.seasonsCaption)
        Spacer(Modifier.height(16.dp))
        Text(
            details.title,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            maxLines = 2
        )
        Spacer(Modifier.height(14.dp))
        MetaRow(details, line = details.metaLineNoDuration)
        Spacer(Modifier.height(16.dp))
        Text(
            details.plot,
            fontSize = 17.sp,
            lineHeight = 25.sp,
            color = Color.White.copy(alpha = 0.85f),
            maxLines = 4
        )

        val resume = details.resume
        if (resume?.progress != null) {
            Spacer(Modifier.height(22.dp))
            ResumeStrip(resume = resume)
        }

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TvButton(
                text = if (resume != null) "Продолжить" else "Смотреть",
                onClick = actions.onPlay,
                leadingIcon = Icons.Filled.PlayArrow,
                focusRequester = actions.playFocus,
            )
            TvButton(
                text = if (isFav) "В избранном" else "В избранное",
                onClick = actions.onToggleFav,
                primary = false,
                leadingIcon = if (isFav) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
            )
        }
    }
}

@Composable
private fun SeriesEpisodesColumn(
    seasons: List<SeasonUi>,
    selectedSeasonIndex: Int,
    currentSeason: SeasonUi?,
    onSelectSeason: (Int) -> Unit,
    onPlayEpisode: (videoId: Int) -> Unit,
) {
    Column(modifier = Modifier.width(380.dp)) {
        if (seasons.size > 1) {
            SeasonChips(
                seasons = seasons,
                selectedSeasonIndex = selectedSeasonIndex,
                onSelect = onSelectSeason,
            )
            Spacer(Modifier.height(16.dp))
        }
        Label(
            currentSeason?.countLabel.orEmpty(),
            color = Color.White.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(4.dp))
        LazyColumn(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(currentSeason?.episodes.orEmpty(), key = { it.id }) { episode ->
                EpisodeRow(
                    episode = episode,
                    onClick = { onPlayEpisode(episode.id) },
                )
            }
        }
    }
}

/** Полоска «Вы остановились» с номером эпизода и прогрессом. */
@Composable
private fun ResumeStrip(resume: ResumeUi) {
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
            Text("${resume.episodeNumber}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
        Column(modifier = Modifier.weight(1f)) {
            Label("Вы остановились")
            Text(
                resume.positionLabel,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
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
                        .fillMaxWidth(resume.progress ?: 0f)
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
    seasons: List<SeasonUi>,
    selectedSeasonIndex: Int,
    onSelect: (Int) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        seasons.forEachIndexed { index, season ->
            SeasonChip(
                label = season.chipLabel,
                active = index == selectedSeasonIndex,
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
private fun EpisodeRow(episode: EpisodeUi, onClick: () -> Unit) {
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
                        contentDescription = episode.label,
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
                    episode.label,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    episode.metaLine,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

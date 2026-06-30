package com.filmax.feature.player.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.filmax.core.tv.designsystem.TvFocusCard
import com.filmax.feature.player.common.PlayerEvent
import com.filmax.feature.player.common.PlayerScreenModel
import com.filmax.feature.player.common.PlayerState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import org.koin.androidx.compose.koinViewModel

/** Категория настроек, показываемая в всплывающей панели выбора. */
private enum class SettingsCategory(val title: String) {
    Quality("Качество"),
    Audio("Аудиодорожка"),
    Subtitle("Субтитры"),
}

/**
 * TV-Плеер (экран 07 макета): видеоповерхность ExoPlayer + центральные контролы под пульт.
 * Снизу — чипы выбора качества/аудио/субтитров; панель выбора всплывает над нажатым чипом.
 * «Назад» прячет интерфейс (повторное «Назад» — выход). Поверх общего [PlayerScreenModel].
 */
@Composable
fun TvPlayerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    screenModel: PlayerScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()
    var progress by remember { mutableFloatStateOf(0f) }
    var controlsVisible by remember { mutableStateOf(true) }
    var openCategory by remember { mutableStateOf<SettingsCategory?>(null) }

    // Позиции чипов (в координатах корня) — чтобы поставить панель ровно над нажатым.
    var chipsRowTop by remember { mutableIntStateOf(0) }
    val chipLefts = remember { mutableStateMapOf<SettingsCategory, Int>() }

    val playFocus = remember { FocusRequester() }
    val hiddenFocus = remember { FocusRequester() }

    // Тик прогресса + сохранение позиции (как на телефоне).
    LaunchedEffect(screenModel.player) {
        while (true) {
            delay(1000)
            val duration = screenModel.player.duration.takeIf { it > 0 } ?: continue
            progress = screenModel.player.currentPosition / duration.toFloat()
            screenModel.dispatch(PlayerEvent.SaveProgress(screenModel.player.currentPosition))
        }
    }

    // Фокус: видимы контролы — на «play», скрыты — на невидимый перехватчик клавиш.
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) runCatching { playFocus.requestFocus() }
        else runCatching { hiddenFocus.requestFocus() }
    }

    // «Назад»: сначала закрыть панель, потом спрятать UI, и только затем выйти из плеера.
    BackHandler {
        when {
            openCategory != null -> openCategory = null
            controlsVisible -> controlsVisible = false
            else -> onBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).also { view ->
                    view.player = screenModel.player
                    view.useController = false
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (state.loading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Center))
        }

        // UI скрыт — невидимый фокусируемый слой возвращает контролы на любое нажатие.
        if (!controlsVisible) {
            Box(
                Modifier
                    .fillMaxSize()
                    .focusRequester(hiddenFocus)
                    .focusable()
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            controlsVisible = true
                            true
                        } else {
                            false
                        }
                    },
            )
        }

        if (controlsVisible) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xB3000000), Color(0x22000000), Color(0x22000000), Color(0xCC000000))
                        )
                    ),
            ) {
                // Сверху — заголовок
                Column(Modifier.align(Alignment.TopStart).padding(56.dp)) {
                    Text("СЕЙЧАС ИГРАЕТ", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(state.item?.title ?: "", color = Color.White, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
                }

                // Центр — контролы перемотки/паузы (размеры по дизайну)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ControlButton(size = 52.dp, icon = Icons.Filled.Replay10, onClick = { screenModel.player.seekBack() })
                    ControlButton(
                        size = 68.dp,
                        icon = if (screenModel.player.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        primary = true,
                        focusRequester = playFocus,
                        onClick = { if (screenModel.player.isPlaying) screenModel.player.pause() else screenModel.player.play() },
                    )
                    ControlButton(size = 52.dp, icon = Icons.Filled.Forward10, onClick = { screenModel.player.seekForward() })
                }

                // Снизу — прогресс-бар + чипы настроек
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 72.dp, vertical = 48.dp)) {
                    val duration = screenModel.player.duration.takeIf { it > 0 } ?: 0L
                    val current = (progress * duration).toLong()
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text(formatMs(current), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Box(
                            Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.22f)),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                        }
                        Text("-${formatMs(duration - current)}", color = Color.White.copy(alpha = 0.7f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    SettingsChips(
                        state = state,
                        modifier = Modifier.padding(top = 24.dp),
                        onOpen = { openCategory = it },
                        onRowPositioned = { chipsRowTop = it },
                        onChipPositioned = { category, left -> chipLefts[category] = left },
                    )
                }

                // Панель выбора — всплывает над нажатым чипом.
                openCategory?.let { category ->
                    SettingsPopover(
                        category = category,
                        state = state,
                        chipLeft = chipLefts[category] ?: 0,
                        chipsRowTop = chipsRowTop,
                        modifier = Modifier.align(Alignment.TopStart),
                        onSelect = { label ->
                            screenModel.dispatch(category.toEvent(label))
                            openCategory = null
                        },
                    )
                }
            }
        }
    }
}

/** Ряд чипов настроек: показываем только те, где реально есть из чего выбрать. */
@Composable
private fun SettingsChips(
    state: PlayerState,
    onOpen: (SettingsCategory) -> Unit,
    onRowPositioned: (top: Int) -> Unit,
    onChipPositioned: (SettingsCategory, left: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.onGloballyPositioned { onRowPositioned(it.positionInRoot().y.roundToInt()) },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.qualities.size > 1) {
            SettingChip("Качество", state.currentQuality ?: "Авто", onClick = { onOpen(SettingsCategory.Quality) }, onPositioned = { onChipPositioned(SettingsCategory.Quality, it) })
        }
        if (state.audioTracks.size > 1) {
            SettingChip("Аудио", state.currentAudio, onClick = { onOpen(SettingsCategory.Audio) }, onPositioned = { onChipPositioned(SettingsCategory.Audio, it) })
        }
        if (state.subtitles.size > 1) {
            SettingChip("Субтитры", state.currentSubtitle, onClick = { onOpen(SettingsCategory.Subtitle) }, onPositioned = { onChipPositioned(SettingsCategory.Subtitle, it) })
        }
    }
}

@Composable
private fun SettingChip(
    label: String,
    value: String,
    onClick: () -> Unit,
    onPositioned: (left: Int) -> Unit,
) {
    TvFocusCard(
        onClick = onClick,
        shape = CircleShape,
        modifier = Modifier.onGloballyPositioned { onPositioned(it.positionInRoot().x.roundToInt()) },
    ) {
        Row(
            Modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(label, color = Color.White.copy(alpha = 0.65f), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Всплывающий список вариантов, спозиционированный над нажатым чипом ([chipLeft]/[chipsRowTop]
 * — в координатах корня). По высоте панель встаёт так, чтобы её низ был чуть выше ряда чипов;
 * по горизонтали прижимается к чипу, но не вылезает за край экрана.
 */
@Composable
private fun SettingsPopover(
    category: SettingsCategory,
    state: PlayerState,
    chipLeft: Int,
    chipsRowTop: Int,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.roundToPx() }
    val gapPx = with(density) { 12.dp.roundToPx() }
    val marginPx = with(density) { 24.dp.roundToPx() }

    var panelWidth by remember(category) { mutableIntStateOf(0) }
    var panelHeight by remember(category) { mutableIntStateOf(0) }

    SettingsPanel(
        category = category,
        state = state,
        onSelect = onSelect,
        modifier = modifier
            .offset {
                val maxX = (screenWidthPx - panelWidth - marginPx).coerceAtLeast(marginPx)
                IntOffset(
                    x = chipLeft.coerceIn(marginPx, maxX),
                    y = (chipsRowTop - panelHeight - gapPx).coerceAtLeast(0),
                )
            }
            .onSizeChanged { panelWidth = it.width; panelHeight = it.height },
    )
}

/** Карточка списка вариантов выбранной категории; первый фокус — на текущем значении. */
@Composable
private fun SettingsPanel(
    category: SettingsCategory,
    state: PlayerState,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = category.options(state)
    val selected = category.selected(state)
    val selectedFocus = remember(category) { FocusRequester() }

    LaunchedEffect(category) { runCatching { selectedFocus.requestFocus() } }

    Column(
        modifier = modifier
            .width(380.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xF21A1518))
            .padding(vertical = 16.dp),
    ) {
        Text(
            category.title,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        options.forEach { option ->
            val isSelected = option == selected
            SettingsRow(
                label = option,
                selected = isSelected,
                onClick = { onSelect(option) },
                focusRequester = if (isSelected) selectedFocus else null,
            )
        }
    }
}

@Composable
private fun SettingsRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    focusRequester: FocusRequester?,
) {
    TvFocusCard(onClick = onClick, shape = RoundedCornerShape(14.dp), focusRequester = focusRequester) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                modifier = Modifier.size(22.dp),
            )
            Text(
                label,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun ControlButton(
    size: Dp,
    icon: ImageVector,
    onClick: () -> Unit,
    primary: Boolean = false,
    focusRequester: FocusRequester? = null,
) {
    TvFocusCard(onClick = onClick, shape = CircleShape, focusRequester = focusRequester, modifier = Modifier.size(size)) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(if (primary) MaterialTheme.colorScheme.primaryContainer else Color.White.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (primary) MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
                modifier = Modifier.size(if (primary) 30.dp else 22.dp),
            )
        }
    }
}

private fun SettingsCategory.options(state: PlayerState): List<String> = when (this) {
    SettingsCategory.Quality -> state.qualities.map { it.label }
    SettingsCategory.Audio -> state.audioTracks.map { it.label }
    SettingsCategory.Subtitle -> state.subtitles.map { it.label }
}

private fun SettingsCategory.selected(state: PlayerState): String = when (this) {
    SettingsCategory.Quality -> state.currentQuality ?: ""
    SettingsCategory.Audio -> state.currentAudio
    SettingsCategory.Subtitle -> state.currentSubtitle
}

private fun SettingsCategory.toEvent(label: String): PlayerEvent = when (this) {
    SettingsCategory.Quality -> PlayerEvent.SelectQuality(label)
    SettingsCategory.Audio -> PlayerEvent.SelectAudio(label)
    SettingsCategory.Subtitle -> PlayerEvent.SelectSubtitle(label)
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

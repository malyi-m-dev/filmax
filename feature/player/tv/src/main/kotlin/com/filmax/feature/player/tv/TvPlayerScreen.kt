package com.filmax.feature.player.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.filmax.core.tv.designsystem.TvFocusCard
import com.filmax.feature.player.common.PlayerEvent
import com.filmax.feature.player.common.PlayerScreenModel
import com.filmax.feature.player.common.PlayerState
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

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
// Единая композиция TV-плеера: verified onScrubKey/commitScrub, эффекты прогресса/SaveProgress
// и фокус-навигация пультом (#22). Дробление рискует регрессией перемотки/скраббинга — подавляем.
@Suppress("LongMethod", "CyclomaticComplexMethod")
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

    // Скраббинг: позиция thumb, которую двигает DPAD до подтверждения seekTo.
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubProgress by remember { mutableFloatStateOf(0f) }
    val scrubberFocus = remember { FocusRequester() }

    // Подтвердить перемотку: seekTo на целевую позицию и выйти из режима скраббинга.
    fun commitScrub() {
        val durationMs = screenModel.player.duration
        if (durationMs > 0) {
            screenModel.player.seekTo((scrubProgress * durationMs).toLong())
            progress = scrubProgress
        }
        isScrubbing = false
    }

    // Позиции чипов (в координатах корня) — чтобы поставить панель ровно над нажатым.
    var chipsRowTop by remember { mutableIntStateOf(0) }
    val chipLefts = remember { mutableStateMapOf<SettingsCategory, Int>() }

    val playFocus = remember { FocusRequester() }
    val hiddenFocus = remember { FocusRequester() }
    // Цель фокуса «вниз» с центральных контролов — первый чип настроек (чтобы «Качество» легко находилось).
    val firstChipFocus = remember { FocusRequester() }
    val hasChips = state.qualities.size > 1 || state.audioTracks.size > 1 || state.subtitles.size > 1

    // Тик прогресса + сохранение позиции (как на телефоне). Во время скраббинга progress не трогаем.
    LaunchedEffect(screenModel.player) {
        while (true) {
            delay(PROGRESS_TICK_MS)
            val duration = screenModel.player.duration.takeIf { it > 0 } ?: continue
            if (!isScrubbing) {
                progress = screenModel.player.currentPosition / duration.toFloat()
            }
            screenModel.dispatch(PlayerEvent.SaveProgress(screenModel.player.currentPosition))
        }
    }

    // Скраббинг подтверждается по таймауту бездействия (либо DPAD_CENTER в обработчике клавиш).
    LaunchedEffect(isScrubbing, scrubProgress) {
        if (isScrubbing) {
            delay(SCRUB_COMMIT_TIMEOUT_MS)
            commitScrub()
        }
    }

    // Фокус: видимы контролы — на «play», скрыты — на невидимый перехватчик клавиш.
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            runCatching { playFocus.requestFocus() }
        } else {
            runCatching { hiddenFocus.requestFocus() }
        }
    }

    // «Назад»: отменить скраббинг, затем закрыть панель, спрятать UI, и только потом выйти из плеера.
    BackHandler {
        when {
            isScrubbing -> isScrubbing = false
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
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // UI скрыт — невидимый фокусируемый слой возвращает контролы на любое нажатие.
        if (!controlsVisible) {
            Box(
                Modifier
                    .fillMaxSize()
                    .focusRequester(hiddenFocus)
                    .focusable()
                    .onKeyEvent { event ->
                        when {
                            // «Назад» не перехватываем — пусть уходит в BackHandler и выходит из плеера.
                            event.key == Key.Back -> false
                            event.type == KeyEventType.KeyDown -> {
                                controlsVisible = true
                                true
                            }

                            else -> false
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
                    Text(
                        "СЕЙЧАС ИГРАЕТ",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        state.item?.title.orEmpty(),
                        color = Color.White,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Центр — контролы перемотки/паузы (размеры по дизайну)
                // «Вниз» с любой центральной кнопки ведёт на прогресс-бар (а с него — на чипы настроек).
                val chipDown = Modifier.focusProperties { down = scrubberFocus }
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ControlButton(
                        size = 52.dp,
                        icon = Icons.Filled.Replay10,
                        focus = ControlButtonFocus(modifier = chipDown),
                        onClick = { screenModel.player.seekBack() },
                    )
                    ControlButton(
                        size = 68.dp,
                        icon = if (screenModel.player.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        primary = true,
                        focus = ControlButtonFocus(modifier = chipDown, requester = playFocus),
                        onClick = {
                            if (screenModel.player.isPlaying) screenModel.player.pause() else screenModel.player.play()
                        },
                    )
                    ControlButton(
                        size = 52.dp,
                        icon = Icons.Filled.Forward10,
                        focus = ControlButtonFocus(modifier = chipDown),
                        onClick = { screenModel.player.seekForward() },
                    )
                }

                // Снизу — прогресс-бар (фокусируемый, со скраббингом) + чипы настроек
                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 72.dp, vertical = 48.dp)
                ) {
                    val durationMs = screenModel.player.duration.takeIf { it > 0 } ?: 0L
                    val displayFraction = (if (isScrubbing) scrubProgress else progress).coerceIn(0f, 1f)
                    val currentMs = (displayFraction * durationMs).toLong()
                    val scrubberFocusProps = Modifier.focusProperties {
                        up = playFocus
                        if (hasChips) down = firstChipFocus
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            formatMs(currentMs),
                            color = if (isScrubbing) MaterialTheme.colorScheme.primary else Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        TvScrubBar(
                            fraction = displayFraction,
                            isScrubbing = isScrubbing,
                            targetLabel = formatMs(currentMs),
                            focusModifier = Modifier.focusRequester(scrubberFocus).then(scrubberFocusProps),
                            onKeyEvent = onScrubKey@{ event ->
                                if (event.type != KeyEventType.KeyDown) return@onScrubKey false
                                val durationForScrub = screenModel.player.duration
                                if (durationForScrub <= 0) return@onScrubKey false
                                when (event.key) {
                                    Key.DirectionLeft -> {
                                        if (!isScrubbing) {
                                            scrubProgress = progress
                                            isScrubbing = true
                                        }
                                        val target = (scrubProgress * durationForScrub - SCRUB_STEP_MS)
                                            .coerceIn(0f, durationForScrub.toFloat())
                                        scrubProgress = target / durationForScrub
                                        true
                                    }

                                    Key.DirectionRight -> {
                                        if (!isScrubbing) {
                                            scrubProgress = progress
                                            isScrubbing = true
                                        }
                                        val target = (scrubProgress * durationForScrub + SCRUB_STEP_MS)
                                            .coerceIn(0f, durationForScrub.toFloat())
                                        scrubProgress = target / durationForScrub
                                        true
                                    }

                                    Key.DirectionCenter, Key.Enter -> {
                                        if (isScrubbing) {
                                            commitScrub()
                                            true
                                        } else {
                                            false
                                        }
                                    }

                                    else -> false
                                }
                            },
                        )
                        Text(
                            "-${formatMs(durationMs - currentMs)}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    SettingsChips(
                        state = state,
                        firstChipFocus = firstChipFocus,
                        modifier = Modifier.padding(top = 24.dp),
                        callbacks = SettingsChipsCallbacks(
                            onOpen = { openCategory = it },
                            onRowPositioned = { chipsRowTop = it },
                            onChipPositioned = { category, left -> chipLefts[category] = left },
                        ),
                    )
                }

                // Панель выбора — всплывает над нажатым чипом.
                openCategory?.let { category ->
                    SettingsPopover(
                        category = category,
                        state = state,
                        anchor = IntOffset(chipLefts[category] ?: 0, chipsRowTop),
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

/**
 * Ряд чипов настроек: показываем только те, где реально есть из чего выбрать. Первому
 * видимому чипу отдаём [firstChipFocus] — это цель навигации «вниз» с центральных контролов,
 * чтобы «Качество» (обычно первый) сразу попадало под фокус и легко находилось.
 */
/** Колбэки ряда чипов настроек: открытие категории и репорт геометрии (для позиционирования поповера). */
private data class SettingsChipsCallbacks(
    val onOpen: (SettingsCategory) -> Unit,
    val onRowPositioned: (top: Int) -> Unit,
    val onChipPositioned: (SettingsCategory, left: Int) -> Unit,
)

@Composable
private fun SettingsChips(
    state: PlayerState,
    firstChipFocus: FocusRequester,
    callbacks: SettingsChipsCallbacks,
    modifier: Modifier = Modifier,
) {
    val categories = buildList {
        if (state.qualities.size > 1) add(SettingsCategory.Quality)
        if (state.audioTracks.size > 1) add(SettingsCategory.Audio)
        if (state.subtitles.size > 1) add(SettingsCategory.Subtitle)
    }
    Row(
        modifier = modifier.onGloballyPositioned { callbacks.onRowPositioned(it.positionInRoot().y.roundToInt()) },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        categories.forEachIndexed { index, category ->
            SettingChip(
                label = category.chipLabel,
                value = category.chipValue(state),
                focusRequester = if (index == 0) firstChipFocus else null,
                onClick = { callbacks.onOpen(category) },
                onPositioned = { callbacks.onChipPositioned(category, it) },
            )
        }
    }
}

@Composable
private fun SettingChip(
    label: String,
    value: String,
    onClick: () -> Unit,
    onPositioned: (left: Int) -> Unit,
    focusRequester: FocusRequester? = null,
) {
    TvFocusCard(
        onClick = onClick,
        shape = CircleShape,
        focusRequester = focusRequester,
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
 * Всплывающий список вариантов, спозиционированный над нажатым чипом ([anchor] — левый край
 * чипа и верх ряда в координатах корня). По высоте панель встаёт так, чтобы её низ был выше ряда;
 * по горизонтали прижимается к чипу, но не вылезает за край экрана.
 */
@Composable
private fun SettingsPopover(
    category: SettingsCategory,
    state: PlayerState,
    anchor: IntOffset,
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
                    x = anchor.x.coerceIn(marginPx, maxX),
                    y = (anchor.y - panelHeight - gapPx).coerceAtLeast(0),
                )
            }
            .onSizeChanged {
                panelWidth = it.width
                panelHeight = it.height
            },
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

/** Модификатор и (опц.) фокус центральной кнопки — сгруппированы, чтобы не раздувать сигнатуру. */
private data class ControlButtonFocus(
    val modifier: Modifier = Modifier,
    val requester: FocusRequester? = null,
)

@Composable
private fun ControlButton(
    size: Dp,
    icon: ImageVector,
    onClick: () -> Unit,
    focus: ControlButtonFocus = ControlButtonFocus(),
    primary: Boolean = false,
) {
    TvFocusCard(
        onClick = onClick,
        shape = CircleShape,
        focusRequester = focus.requester,
        modifier = focus.modifier.size(size)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    if (primary) MaterialTheme.colorScheme.primaryContainer else Color.White.copy(alpha = 0.16f)
                ),
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

/**
 * Фокусируемый прогресс-бар со скраббингом: рисует трек, активную часть, thumb-индикатор и
 * (во время скраббинга) пузырёк с целевым временем над thumb. Обработку DPAD (стрелки/центр)
 * пробрасываем наружу через [onKeyEvent], а фокус-обвязку (реквестер + up/down) — через [focusModifier].
 */
@Suppress("LongMethod") // рендер прогресс-бара скраббинга (трек/thumb/пузырёк), верифицирован в #22
@Composable
private fun RowScope.TvScrubBar(
    fraction: Float,
    isScrubbing: Boolean,
    targetLabel: String,
    focusModifier: Modifier,
    onKeyEvent: (KeyEvent) -> Boolean,
) {
    var focused by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = Modifier
            .weight(1f)
            .height(ScrubThumbFocusedSize)
            .then(focusModifier)
            .onFocusChanged { focused = it.isFocused }
            .onKeyEvent(onKeyEvent)
            .focusable(),
    ) {
        val thumbSize = if (focused || isScrubbing) ScrubThumbFocusedSize else ScrubThumbSize
        val travelPx = with(density) { (maxWidth - thumbSize).toPx() }
        val thumbX = (fraction * travelPx).roundToInt()

        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(ScrubTrackHeight)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(ScrubTrackHeight)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }

        val thumbModifier = Modifier
            .align(Alignment.CenterStart)
            .offset { IntOffset(thumbX, 0) }
            .size(thumbSize)
            .clip(CircleShape)
            .background(Color.White)
        Box(
            if (focused) {
                thumbModifier.border(ScrubThumbBorder, MaterialTheme.colorScheme.primary, CircleShape)
            } else {
                thumbModifier
            },
        )

        if (isScrubbing) {
            var bubbleSize by remember { mutableStateOf(IntSize.Zero) }
            val widthPx = with(density) { maxWidth.toPx() }
            val gapPx = with(density) { ScrubBubbleGap.roundToPx() }
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .onSizeChanged { bubbleSize = it }
                    .offset {
                        val thumbCenter = thumbX + thumbSize.toPx() / 2f
                        val maxX = (widthPx - bubbleSize.width).coerceAtLeast(0f)
                        IntOffset(
                            (thumbCenter - bubbleSize.width / 2f).coerceIn(0f, maxX).roundToInt(),
                            -(bubbleSize.height + gapPx),
                        )
                    }
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(targetLabel, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Короткая подпись чипа (в ряду настроек), в отличие от полного [SettingsCategory.title] в поповере. */
private val SettingsCategory.chipLabel: String
    get() = when (this) {
        SettingsCategory.Quality -> "Качество"
        SettingsCategory.Audio -> "Аудио"
        SettingsCategory.Subtitle -> "Субтитры"
    }

private fun SettingsCategory.chipValue(state: PlayerState): String = when (this) {
    SettingsCategory.Quality -> state.currentQuality ?: "Авто"
    SettingsCategory.Audio -> state.currentAudio
    SettingsCategory.Subtitle -> state.currentSubtitle
}

private fun SettingsCategory.options(state: PlayerState): List<String> = when (this) {
    SettingsCategory.Quality -> state.qualities.map { it.label }
    SettingsCategory.Audio -> state.audioTracks.map { it.label }
    SettingsCategory.Subtitle -> state.subtitles.map { it.label }
}

private fun SettingsCategory.selected(state: PlayerState): String = when (this) {
    SettingsCategory.Quality -> state.currentQuality.orEmpty()
    SettingsCategory.Audio -> state.currentAudio
    SettingsCategory.Subtitle -> state.currentSubtitle
}

private fun SettingsCategory.toEvent(label: String): PlayerEvent = when (this) {
    SettingsCategory.Quality -> PlayerEvent.SelectQuality(label)
    SettingsCategory.Audio -> PlayerEvent.SelectAudio(label)
    SettingsCategory.Subtitle -> PlayerEvent.SelectSubtitle(label)
}

/** Тик прогресса/сохранения позиции (мс). */
private const val PROGRESS_TICK_MS = 1000L

/** Шаг перемотки скраббингом за одно нажатие DPAD влево/вправо (мс). */
private const val SCRUB_STEP_MS = 10_000L

/** Бездействие, после которого скраббинг подтверждается и выполняется seekTo (мс). */
private const val SCRUB_COMMIT_TIMEOUT_MS = 700L

private val ScrubTrackHeight = 8.dp
private val ScrubThumbSize = 18.dp
private val ScrubThumbFocusedSize = 26.dp
private val ScrubThumbBorder = 3.dp
private val ScrubBubbleGap = 10.dp

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

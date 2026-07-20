package com.filmax.feature.player.mobile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.filmax.core.ui.components.FilmaxErrorModal
import com.filmax.core.ui.components.KeepScreenOn
import com.filmax.feature.player.common.PlaybackSpeeds
import com.filmax.feature.player.common.PlayerEvent
import com.filmax.feature.player.common.PlayerScreenModel
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

// PlayerScreen — единая композиция экрана плеера: ExoPlayer-поверхность, верифицированный
// Slider скраббинга и эффекты авто-скрытия/SaveProgress (#22). Декомпозиция раздробила бы
// проверенную логику воспроизведения/перемотки/сохранения прогресса — поэтому подавляем.
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    screenModel: PlayerScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()
    val appError by screenModel.collectErrorAsState()
    var controlsVisible by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0f) }
    // Пока пользователь тащит thumb — фоновый тик не перезаписывает progress, seek уходит по завершению жеста.
    var isScrubbing by remember { mutableStateOf(false) }
    var qualityMenu by remember { mutableStateOf(false) }
    var subtitleMenu by remember { mutableStateOf(false) }
    var audioMenu by remember { mutableStateOf(false) }
    var speedMenu by remember { mutableStateOf(false) }

    // Пока идёт воспроизведение, экран не гаснет (жалоба: гас через 10 минут при живом звуке);
    // на паузе — обычный таймаут системы.
    var playing by remember { mutableStateOf(screenModel.player.isPlaying) }
    DisposableEffect(screenModel.player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }
        }
        screenModel.player.addListener(listener)
        onDispose { screenModel.player.removeListener(listener) }
    }
    KeepScreenOn(enabled = playing)

    // Auto-hide controls (но не во время скраббинга — таймер стартует заново после жеста).
    LaunchedEffect(controlsVisible, isScrubbing) {
        if (controlsVisible && !isScrubbing) {
            delay(AUTO_HIDE_DELAY_MS)
            controlsVisible = false
        }
    }
    // Track playback progress
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { controlsVisible = !controlsVisible },
    ) {
        // ── ExoPlayer surface ─────────────────────────────────────────────────
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).also { view ->
                    view.player = screenModel.player
                    view.useController = false
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // ── Loading ───────────────────────────────────────────────────────────
        if (state.loading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        appError?.let { error ->
            FilmaxErrorModal(
                error = error,
                onDismiss = screenModel::dismissError,
                onPrimary = {
                    screenModel.dismissError()
                    onBack()
                },
                onSecondary = onBack,
            )
        }

        // ── Controls overlay ──────────────────────────────────────────────────
        AnimatedVisibility(
            visible = controlsVisible && !state.loading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(Color(0xB3000000), Color(0x33000000), Color(0x33000000), Color(0xCC000000))
                    )
                )
            ) {
                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GlassBtn(size = 44.dp, onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "СЕЙЧАС ИГРАЕТ",
                            fontSize = 11.sp,
                            color = Color.White.copy(0.7f),
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            state.item?.title.orEmpty(),
                            fontSize = 15.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    // Озвучка: показываем только когда есть из чего выбрать (>1 дорожки), как на TV.
                    if (state.audioTracks.size > 1) {
                        Box {
                            GlassBtn(size = 44.dp, onClick = { audioMenu = true }) {
                                Icon(
                                    Icons.Filled.Audiotrack,
                                    contentDescription = "Озвучка",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = audioMenu,
                                onDismissRequest = { audioMenu = false },
                            ) {
                                state.audioTracks.forEach { option ->
                                    val isCurrent = option.label == state.currentAudio
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                option.label,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            )
                                        },
                                        onClick = {
                                            screenModel.dispatch(PlayerEvent.SelectAudio(option.label))
                                            audioMenu = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                    if (state.subtitles.size > 1) {
                        Box {
                            GlassBtn(size = 44.dp, onClick = { subtitleMenu = true }) {
                                Icon(
                                    Icons.Filled.ClosedCaption,
                                    contentDescription = "Субтитры",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = subtitleMenu,
                                onDismissRequest = { subtitleMenu = false },
                            ) {
                                state.subtitles.forEach { option ->
                                    val isCurrent = option.label == state.currentSubtitle
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                option.label,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            )
                                        },
                                        onClick = {
                                            screenModel.dispatch(PlayerEvent.SelectSubtitle(option.label))
                                            subtitleMenu = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                // Center: rewind / play / forward
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassBtn(size = 64.dp, onClick = { screenModel.player.seekBack() }) {
                        Icon(
                            Icons.Filled.Replay10,
                            contentDescription = "Назад 10 сек",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Surface(
                        modifier = Modifier.size(88.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        onClick = {
                            if (screenModel.player.isPlaying) {
                                screenModel.player.pause()
                            } else {
                                screenModel.player.play()
                            }
                        },
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                if (screenModel.player.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(40.dp),
                            )
                        }
                    }
                    GlassBtn(size = 64.dp, onClick = { screenModel.player.seekForward() }) {
                        Icon(
                            Icons.Filled.Forward10,
                            contentDescription = "Вперёд 10 сек",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                // Bottom: scrubber + time + controls
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                ) {
                    val durationMs = screenModel.player.duration.takeIf { it > 0 } ?: 0L
                    val targetMs = (progress * durationMs).toLong()
                    // Превью целевого времени во время перетаскивания (высота зарезервирована — без скачка вёрстки).
                    Box(
                        modifier = Modifier.fillMaxWidth().height(PreviewBarHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isScrubbing) {
                            Text(
                                formatMs(targetMs),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                            )
                        }
                    }
                    Slider(
                        value = progress,
                        onValueChange = { value ->
                            isScrubbing = true
                            progress = value
                        },
                        onValueChangeFinished = {
                            val duration = screenModel.player.duration
                            if (duration > 0) screenModel.player.seekTo((progress * duration).toLong())
                            isScrubbing = false
                        },
                        enabled = durationMs > 0,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(0.2f),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            formatMs(targetMs),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "-${formatMs(durationMs - targetMs)}",
                            color = Color.White.copy(0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (state.qualities.isNotEmpty()) {
                            Box {
                                QualityChip(
                                    text = state.currentQuality ?: "Авто",
                                    onClick = { qualityMenu = true },
                                )
                                DropdownMenu(
                                    expanded = qualityMenu,
                                    onDismissRequest = { qualityMenu = false },
                                ) {
                                    state.qualities.forEach { q ->
                                        val isCurrent = q.label == state.currentQuality
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    q.label,
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                )
                                            },
                                            onClick = {
                                                screenModel.dispatch(PlayerEvent.SelectQuality(q.label))
                                                qualityMenu = false
                                            },
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(Modifier.size(0.dp))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Скорость воспроизведения: галочка отмечает текущую (сессионная, дефолт 1.0).
                            Box {
                                GlassBtn(size = 44.dp, onClick = { speedMenu = true }) {
                                    Icon(
                                        Icons.Filled.Speed,
                                        contentDescription = "Скорость",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = speedMenu,
                                    onDismissRequest = { speedMenu = false },
                                ) {
                                    PlaybackSpeeds.options.forEach { option ->
                                        val isCurrent = option.value == state.currentSpeed
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    option.label,
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                )
                                            },
                                            trailingIcon = if (isCurrent) {
                                                {
                                                    Icon(
                                                        Icons.Filled.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp),
                                                    )
                                                }
                                            } else {
                                                null
                                            },
                                            onClick = {
                                                screenModel.dispatch(PlayerEvent.SetSpeed(option.value))
                                                speedMenu = false
                                            },
                                        )
                                    }
                                }
                            }
                            GlassBtn(size = 44.dp, onClick = {}) {
                                Icon(
                                    Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Громкость",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            GlassBtn(size = 44.dp, onClick = {}) {
                                Icon(
                                    Icons.Filled.SkipNext,
                                    contentDescription = "Далее",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            GlassBtn(size = 44.dp, onClick = {}) {
                                Icon(
                                    Icons.Filled.Fullscreen,
                                    contentDescription = "На весь экран",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val AUTO_HIDE_DELAY_MS = 4500L
private const val PROGRESS_TICK_MS = 1000L
private val PreviewBarHeight = 28.dp

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

@Composable
private fun GlassBtn(size: Dp = 52.dp, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(Color(0x1FFFFFFF)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun QualityChip(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color(0x26FFFFFF))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

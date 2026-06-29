package com.filmax.feature.player

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.koin.androidx.compose.koinViewModel
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    screenModel: PlayerScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()
    var controlsVisible by remember { mutableStateOf(true) }
    var progress by remember { mutableFloatStateOf(0f) }

    // Auto-hide controls after 4.5s
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(4500)
            controlsVisible = false
        }
    }
    // Track playback progress
    LaunchedEffect(screenModel.player) {
        while (true) {
            delay(1000)
            val duration = screenModel.player.duration.takeIf { it > 0 } ?: continue
            progress = screenModel.player.currentPosition / duration.toFloat()
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassBtn(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    Column {
                        Text("Сейчас играет", fontSize = 11.sp, color = Color.White.copy(0.7f), fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                        Text(state.item?.title ?: "", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                // Center play/pause
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassBtn(onClick = { screenModel.player.seekBack() }) {
                        Text("⏪", fontSize = 22.sp)
                    }
                    // Big play button
                    Surface(
                        modifier = Modifier.size(88.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        onClick = {
                            if (screenModel.player.isPlaying) screenModel.player.pause()
                            else screenModel.player.play()
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
                    GlassBtn(onClick = { screenModel.player.seekForward() }) {
                        Text("⏩", fontSize = 22.sp)
                    }
                }

                // Bottom: scrubber + time
                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp, vertical = 28.dp),
                ) {
                    Slider(
                        value = progress,
                        onValueChange = { v ->
                            progress = v
                            val duration = screenModel.player.duration.takeIf { it > 0 } ?: return@Slider
                            screenModel.player.seekTo((v * duration).toLong())
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            inactiveTrackColor = Color.White.copy(0.2f),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val duration = screenModel.player.duration.takeIf { it > 0 } ?: 0L
                        val current = (progress * duration).toLong()
                        Text(formatMs(current), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("-${formatMs(duration - current)}", color = Color.White.copy(0.7f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

@Composable
private fun GlassBtn(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(52.dp).clip(CircleShape).background(Color(0x1FFFFFFF)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

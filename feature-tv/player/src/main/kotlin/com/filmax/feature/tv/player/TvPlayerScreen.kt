package com.filmax.feature.tv.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.filmax.core.tv.designsystem.TvFocusCard
import com.filmax.feature.player.PlayerEvent
import com.filmax.feature.player.PlayerScreenModel
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

/**
 * TV-Плеер (экран 07 макета): видеоповерхность ExoPlayer + центральные контролы под пульт.
 * Поверх общего [PlayerScreenModel] (подготовка потока/прогресс/качество — те же, что и на телефоне).
 */
@Composable
fun TvPlayerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    screenModel: PlayerScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()
    var progress by remember { mutableFloatStateOf(0f) }

    // Тик прогресса + сохранение позиции (как на телефоне).
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

        // Затемнение + контролы
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

            // Центр — контролы перемотки/паузы
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ControlButton(size = 80.dp, icon = Icons.Filled.Replay10, onClick = { screenModel.player.seekBack() })
                ControlButton(
                    size = 104.dp,
                    icon = if (screenModel.player.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    primary = true,
                    onClick = { if (screenModel.player.isPlaying) screenModel.player.pause() else screenModel.player.play() },
                )
                ControlButton(size = 80.dp, icon = Icons.Filled.Forward10, onClick = { screenModel.player.seekForward() })
            }

            // Снизу — прогресс-бар
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
            }
        }
    }
}

@Composable
private fun ControlButton(
    size: androidx.compose.ui.unit.Dp,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    primary: Boolean = false,
) {
    TvFocusCard(onClick = onClick, shape = CircleShape, modifier = Modifier.size(size)) {
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
                modifier = Modifier.size(if (primary) 44.dp else 32.dp),
            )
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

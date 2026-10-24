// Громкость в оверлее плеера: вертикальная стеклянная дорожка у правого края. Меняет громкость
// медиа в системе — ту же, что кнопки телефона, а не громкость одного плеера.

package com.filmax.feature.player.mobile

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.glassFill
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Как часто сверяться с системной громкостью: её меняют и кнопки телефона. */
private const val VOLUME_SYNC_MS = 500L
private const val PERCENT = 100

/** С какой громкости иконка ложится на светлую заливку и темнеет. */
private const val DARK_ICON_LEVEL = 0.18f

/**
 * Дорожка громкости: заливка снизу по уровню, тап или протяжка пальцем ставят уровень. Пока
 * оверлей на экране, дорожка следит за системной громкостью — нажали кнопку телефона, она догнала.
 */
@Composable
internal fun PlayerVolume(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val audio = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val max = remember(audio) { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var level by remember { mutableFloatStateOf(audio.mediaLevel(max)) }
    LaunchedEffect(audio) {
        while (true) {
            delay(VOLUME_SYNC_MS)
            level = audio.mediaLevel(max)
        }
    }
    val setLevel: (Float) -> Unit = { wanted ->
        level = wanted.coerceIn(0f, 1f)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, (level * max).roundToInt(), 0)
    }
    Box(
        modifier
            .size(width = 44.dp, height = 132.dp)
            .glassFill(TrackShape)
            .pointerInput(max) {
                detectTapGestures { offset -> setLevel(1f - offset.y / size.height) }
            }
            .pointerInput(max) {
                detectVerticalDragGestures { change, _ -> setLevel(1f - change.position.y / size.height) }
            }
            .semantics {
                contentDescription = "Громкость"
                stateDescription = "${(level * PERCENT).roundToInt()}%"
            },
    ) {
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(level)
                .clip(TrackShape)
                .background(Fill),
        )
        Icon(
            if (level > 0f) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
            contentDescription = null,
            tint = if (level > DARK_ICON_LEVEL) IconOnFill else Color.White,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 11.dp).size(22.dp),
        )
    }
}

private fun AudioManager.mediaLevel(max: Int): Float = getStreamVolume(AudioManager.STREAM_MUSIC) / max.toFloat()

private val TrackShape = RoundedCornerShape(22.dp)
private val Fill = Color.White.copy(alpha = 0.88f)
private val IconOnFill = Color(0xFF14111F)

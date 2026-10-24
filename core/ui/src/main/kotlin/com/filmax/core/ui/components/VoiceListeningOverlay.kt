package com.filmax.core.ui.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.glow
import com.filmax.core.designsystem.radialWash

/**
 * Голосовой поиск на весь экран: «Говорите…», текст речи по ходу, пульсирующий микрофон и
 * эквалайзер громкости, подсказки-примеры. Окно поверх всего — его можно открыть из любого места
 * дерева; «Назад», «✕» и тап по примеру закрывают слушание. [onExample] — пример выбран тапом:
 * экран ищет его так же, как сказанное.
 */
@Composable
fun VoiceListeningOverlay(
    controller: VoiceSearchController,
    examples: List<String> = emptyList(),
    onExample: (String) -> Unit = {},
) {
    if (!controller.active) return
    Dialog(
        onDismissRequest = controller::cancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val palette = FilmaxTheme.palette
        Box(
            Modifier
                .fillMaxSize()
                .background(palette.background.copy(alpha = BACKDROP_ALPHA))
                .radialWash(palette.wash, centerX = 0.5f, centerY = 0.58f, radiusX = 0.7f, radiusY = 0.45f),
        ) {
            ListeningColumn(
                controller = controller,
                examples = examples,
                onExample = { example ->
                    controller.cancel()
                    onExample(example)
                },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = ListeningTop),
            )
            FilmaxIconButton(
                Icons.Filled.Close,
                contentDescription = "Закрыть",
                onClick = controller::cancel,
                size = CloseSize,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = CloseBottom),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ListeningColumn(
    controller: VoiceSearchController,
    examples: List<String>,
    onExample: (String) -> Unit,
    modifier: Modifier,
) {
    val error = controller.error
    Column(
        modifier.fillMaxWidth().padding(horizontal = FilmaxMetrics.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (error != null) voiceErrorText(error) else "Говорите…",
            style = FilmaxType.label,
            color = if (error != null) FilmaxColors.Error else FilmaxColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Transcript(controller.transcript, Modifier.padding(top = 14.dp))
        MicPulse(listening = controller.listening, modifier = Modifier.padding(top = 48.dp))
        VoiceLevel(level = controller.level, modifier = Modifier.padding(top = 36.dp))
        if (examples.isNotEmpty()) {
            Text(
                "Попробуйте сказать",
                style = FilmaxType.meta,
                color = FilmaxColors.TextDim,
                modifier = Modifier.padding(top = 40.dp, bottom = 12.dp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                examples.forEach { example ->
                    FilmaxChip(label = "«$example»", selected = false, onClick = { onExample(example) })
                }
            }
        }
    }
}

/** Подтверждённая часть — в полную силу, догадка распознавателя — бледнее: она ещё поменяется. */
@Composable
private fun Transcript(transcript: VoiceTranscript, modifier: Modifier) {
    if (transcript.text.isBlank()) return
    Text(
        buildAnnotatedString {
            append(transcript.confirmed)
            withStyle(SpanStyle(color = FilmaxColors.TextDim)) { append(transcript.hypothesis) }
        },
        style = FilmaxType.headline,
        color = FilmaxColors.Text,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

/** Микрофон в акцентном круге и три кольца, расходящиеся по очереди, пока идёт слушание. */
@Composable
private fun MicPulse(listening: Boolean, modifier: Modifier) {
    val palette = FilmaxTheme.palette
    Box(modifier.size(RingSize * RING_MAX_SCALE), contentAlignment = Alignment.Center) {
        if (listening && rememberAnimationsEnabled()) {
            val transition = rememberInfiniteTransition(label = "micRings")
            repeat(RING_COUNT) { ring ->
                val phase by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        tween(RING_PERIOD_MILLIS, easing = LinearOutSlowInEasing),
                        RepeatMode.Restart,
                        StartOffset(ring * RING_DELAY_MILLIS),
                    ),
                    label = "micRing$ring",
                )
                Box(
                    Modifier
                        .size(RingSize)
                        .graphicsLayer {
                            val scale = 1f + (RING_MAX_SCALE - 1f) * phase
                            scaleX = scale
                            scaleY = scale
                            alpha = 1f - phase
                        }
                        .border(2.dp, palette.accent, CircleShape),
                )
            }
        }
        Box(
            Modifier
                .size(MicSize)
                .glow(palette.glow, CircleShape, blur = 50.dp, spread = 0.dp, offsetY = 10.dp)
                .clip(CircleShape)
                .background(palette.accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Mic, contentDescription = null, tint = palette.onAccent, modifier = Modifier.size(48.dp))
        }
    }
}

/** Эквалайзер: семь столбиков, высота — от громкости речи, середина выше краёв. */
@Composable
private fun VoiceLevel(level: Float, modifier: Modifier) {
    val palette = FilmaxTheme.palette
    Row(
        modifier.height(BarMaxHeight),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BarWeights.forEach { weight ->
            val height by animateDpAsState(
                BarMinHeight + (BarMaxHeight - BarMinHeight) * (level * weight),
                label = "bar"
            )
            Box(
                Modifier
                    .width(5.dp)
                    .height(height)
                    .clip(BarShape)
                    .background(palette.accentText),
            )
        }
    }
}

private val ListeningTop = 150.dp
private val CloseSize = 56.dp
private val CloseBottom = 40.dp
private val MicSize = 104.dp
private val RingSize = 120.dp
private val BarMinHeight = 6.dp
private val BarMaxHeight = 40.dp
private val BarShape = RoundedCornerShape(3.dp)

/** Доли громкости столбиков: середина эквалайзера отзывается сильнее краёв. */
private val BarWeights = listOf(0.45f, 0.7f, 0.9f, 1f, 0.9f, 0.7f, 0.45f)
private const val RING_COUNT = 3
private const val RING_MAX_SCALE = 1.6f
private const val RING_PERIOD_MILLIS = 1_980
private const val RING_DELAY_MILLIS = 660
private const val BACKDROP_ALPHA = 0.9f

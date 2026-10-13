// Слушание голосового поиска: микрофон с расходящимися кольцами, эквалайзер громкости и текст,
// который распознаётся по ходу речи. Оверлей из них собирает экран.
package com.filmax.core.tv.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.PI
import kotlin.math.sin

private val MicCircleSize = 120.dp
private val MicGlyphSize = 52.dp
private val RingStroke = 2.dp

/** Кольца: расходятся до 1.9 размера круга и гаснут с 0.55 за 2 с, по одному каждые 660 мс. */
private const val RING_COUNT = 3
private const val RING_MS = 2_000
private const val RING_MAX_SCALE = 1.9f
private const val RING_START_ALPHA = 0.55f

/** Свечение круга выходит за него на 40dp (box-shadow 60px 10px макета). */
private val GlowReach = 40.dp

/** Круг появляется с перелётом — с этого размера. */
private const val POP_FROM = 0.6f

/** Гипотеза распознавания — бледнее подтверждённого текста. */
private const val HYPOTHESIS_ALPHA = 0.4f
private val TranscriptCaretWidth = 3.dp
private val TranscriptCaretHeight = 30.dp

/**
 * Микрофон слушания — круг 120dp акцентом со свечением и тремя расходящимися кольцами. [error] —
 * слушать не вышло (нет сети, нет доступа к микрофону): перечёркнутый микрофон цветом ошибки,
 * без колец — ждать нечего.
 */
@Composable
fun TvMicPulse(error: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalFxColors.current
    val pop = rememberPop()
    Box(modifier.size(MicCircleSize), contentAlignment = Alignment.Center) {
        if (!error) Rings()
        Box(
            Modifier
                .size(MicCircleSize)
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                }
                .then(if (error) Modifier else Modifier.micGlow(colors.glow))
                .background(if (error) colors.surfaceHigh else colors.accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (error) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                contentDescription = null,
                tint = if (error) colors.error else colors.onAccent,
                modifier = Modifier.size(MicGlyphSize),
            )
        }
    }
}

/** Появление круга по [FxMotion.pop]: с 0.6 до 1 с перелётом; при сниженной анимации — сразу. */
@Composable
private fun rememberPop(): State<Float> {
    val reduced = LocalReducedMotion.current
    val scale = remember { Animatable(if (reduced) 1f else POP_FROM) }
    LaunchedEffect(Unit) { scale.animateTo(1f, FxMotion.pop()) }
    return scale.asState()
}

/** Мягкое свечение палитры вокруг круга — радиальным градиентом, без размытия слоя. */
private fun Modifier.micGlow(glow: Color): Modifier = drawBehind {
    val radius = size.minDimension / 2 + GlowReach.toPx()
    drawCircle(Brush.radialGradient(listOf(glow, Color.Transparent), center = center, radius = radius), radius)
}

/** Три кольца одной фазой: каждое — со своим сдвигом, анимация читается только при отрисовке. */
@Composable
private fun Rings() {
    if (LocalReducedMotion.current) return
    val accent = LocalFxColors.current.accent
    val transition = rememberInfiniteTransition(label = "voiceRings")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(RING_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "voiceRingsPhase",
    )
    Canvas(Modifier.size(MicCircleSize)) {
        repeat(RING_COUNT) { ring -> drawRing(ringProgress(phase.value, ring), accent) }
    }
}

/** Ход кольца [ring] при общей фазе [phase]: кольца идут через равные трети цикла. */
private fun ringProgress(phase: Float, ring: Int): Float =
    FxEasing.Standard.transform((phase + ring.toFloat() / RING_COUNT) % 1f)

private fun DrawScope.drawRing(progress: Float, color: Color) {
    val scale = 1f + (RING_MAX_SCALE - 1f) * progress
    drawCircle(
        color = color.copy(alpha = RING_START_ALPHA * (1f - progress)),
        radius = (size.minDimension / 2) * scale,
        style = Stroke(width = RingStroke.toPx()),
    )
}

private val BarWidth = 4.dp
private val BarGap = 4.dp
private val LevelHeight = 40.dp
private val BarRadius = 2.dp

/** Профиль полос из макета: разная высота, чтобы эквалайзер читался как звук, а не как столбик. */
private val BarProfile = listOf(18, 29, 13, 36, 22, 32, 16, 27, 11, 34, 20, 25, 30, 15).map { it / 36f }

/** Высота полосы — от 25% до полной, как scaleY 0.25–1 в макете. */
private const val BAR_MIN = 0.25f

/** Полосы дышат вразнобой и при ровном голосе: доля колебания и его период. */
private const val WOBBLE = 0.35f
private const val WOBBLE_MS = 900
private const val LEVEL_FOLLOW_MS = 90

/**
 * Эквалайзер слушания: 14 полос, высота — от громкости речи [level] (0..1). Громкость приходит
 * десятки раз в секунду, поэтому полосы читают её при отрисовке и догоняют плавно — экран от
 * этого не перекомпоновывается.
 */
@Composable
fun TvVoiceLevel(level: () -> Float, modifier: Modifier = Modifier) {
    val color = LocalFxColors.current.accentText
    val smoothed = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        snapshotFlow(level).collectLatest { target -> smoothed.animateTo(target, tween(LEVEL_FOLLOW_MS)) }
    }
    val wobble = rememberWobble()
    val width = BarWidth * BarProfile.size + BarGap * (BarProfile.size - 1)
    Canvas(modifier.size(width, LevelHeight)) {
        BarProfile.forEachIndexed { index, profile ->
            val scale = BAR_MIN + (1f - BAR_MIN) * smoothed.value * barSwing(wobble.value, index)
            drawBar(index, size.height * profile * scale, color)
        }
    }
}

/** Колебание полосы [index] при фазе [phase]: соседние полосы — в разных точках волны. */
private fun barSwing(phase: Float, index: Int): Float {
    val wave = sin(2 * PI.toFloat() * (phase + index.toFloat() / BarProfile.size))
    return 1f - WOBBLE + WOBBLE * wave
}

@Composable
private fun rememberWobble(): State<Float> {
    val transition = rememberInfiniteTransition(label = "voiceWobble")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = if (LocalReducedMotion.current) 0f else 1f,
        animationSpec = infiniteRepeatable(tween(WOBBLE_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "voiceWobblePhase",
    )
}

/** Полоса [index] высотой [height] по центру по вертикали. */
private fun DrawScope.drawBar(index: Int, height: Float, color: Color) {
    val left = index * (BarWidth + BarGap).toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(left, (size.height - height) / 2),
        size = Size(BarWidth.toPx(), height),
        cornerRadius = CornerRadius(BarRadius.toPx()),
    )
}

/**
 * Текст по ходу речи: [confirmed] — в полную силу, [hypothesis] — бледно (распознаватель ещё может
 * его переиграть), за ним — курсор.
 */
@Composable
fun TvVoiceTranscript(confirmed: String, hypothesis: String, modifier: Modifier = Modifier) {
    val colors = LocalFxColors.current
    val text = buildAnnotatedString {
        append(confirmed)
        withStyle(SpanStyle(color = colors.text.copy(alpha = HYPOTHESIS_ALPHA))) { append(hypothesis) }
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = FxType.screenTitle, color = colors.text, textAlign = TextAlign.Center, maxLines = 2)
        TvCaret(TranscriptCaretWidth, TranscriptCaretHeight)
    }
}

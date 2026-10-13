// Карточка подписки Профиля: плашка акцентом, крупный остаток дней, блик и медленно вращающаяся
// рамка в углу. Эффекты — бесконечные, поэтому только пока их видно и не в облегчённом режиме.
package com.filmax.core.tv.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.tv.material3.Text

private val CardWidth = 290.dp
private val CardHeight = 228.dp
private val CardPadding = 20.dp
private val CardShape = RoundedCornerShape(20.dp)
private val ValueStyle = FxType.hero.copy(
    fontWeight = FontWeight.ExtraBold,
    fontSize = 48.sp,
    lineHeight = 48.sp,
    letterSpacing = (-0.03).em,
)
private val OverlineStyle = FxType.overline.copy(fontWeight = FontWeight.Bold)
private val CaptionStyle = FxType.meta.copy(fontWeight = FontWeight.Medium)
private const val CAPTION_ALPHA = 0.8f

/**
 * Карточка подписки: [overline] («FILMAX PREMIUM»), крупное [value] («47 дней») и [caption]
 * («активна до 29 ноября»). Поверх цвета идёт блик, в правом верхнем углу медленно вращается
 * рамка — как в макете; в облегчённом режиме, при сниженной анимации и под панелью карточка
 * неподвижна.
 */
@Composable
fun TvSubscriptionCard(overline: String, value: String, caption: String?, modifier: Modifier = Modifier) {
    val colors = LocalFxColors.current
    Column(
        modifier
            .width(CardWidth)
            .height(CardHeight)
            .clip(CardShape)
            .background(colors.accent)
            .subscriptionEffects()
            .padding(CardPadding),
    ) {
        Text(overline.uppercase(), style = OverlineStyle, color = colors.onAccent)
        Text(value, style = ValueStyle, color = colors.onAccent, maxLines = 1, modifier = Modifier.padding(top = 18.dp))
        if (caption != null) {
            Text(
                caption,
                style = CaptionStyle,
                color = colors.onAccent.copy(alpha = CAPTION_ALPHA),
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/** Блик: полоса 40% ширины, наклон 20°, проходит слева направо за 2.6 с после паузы 0.6 с. */
private const val SWEEP_MS = 2_600
private const val SWEEP_DELAY_MS = 600
private const val SWEEP_WIDTH = 0.4f
private const val SWEEP_FROM = -1.2f
private const val SWEEP_TO = 3.2f
private const val SWEEP_SKEW_DEGREES = -20f
private const val SWEEP_ALPHA = 0.25f

/** Рамка в углу: квадрат 180dp со скруглением 40dp, центр на 30dp внутрь от угла, оборот за 9 с. */
private val FrameSize = 180.dp
private val FrameRadius = 40.dp
private val FrameInset = 30.dp
private val FrameStroke = 2.dp
private const val FRAME_ALPHA = 0.25f
private const val SPIN_MS = 9_000
private const val FULL_TURN = 360f

/**
 * Блик и рамка рисуются поверх цвета, под текстом. Прогресс бесконечных анимаций читается при
 * отрисовке — карточка не перекомпонуется; кисть и размеры считаются один раз на размер карточки.
 */
@Composable
private fun Modifier.subscriptionEffects(): Modifier {
    val still = LocalReducedMotion.current || LocalLightEffects.current || LocalFxPaused.current
    val (sweep, spin) = if (still) rememberStillEffects() else rememberRunningEffects()
    return drawWithCache {
        val bandWidth = size.width * SWEEP_WIDTH
        val band = Brush.horizontalGradient(
            listOf(Color.Transparent, Color.White.copy(alpha = SWEEP_ALPHA), Color.Transparent),
            startX = 0f,
            endX = bandWidth,
        )
        val frameSize = FrameSize.toPx()
        val frameCenter = Offset(size.width - FrameInset.toPx(), FrameInset.toPx())
        val frameTopLeft = frameCenter - Offset(frameSize / 2, frameSize / 2)
        val frameStroke = Stroke(width = FrameStroke.toPx())
        onDrawBehind {
            rotate(spin.value * FULL_TURN, pivot = frameCenter) {
                drawRoundRect(
                    color = Color.White.copy(alpha = FRAME_ALPHA),
                    topLeft = frameTopLeft,
                    size = Size(frameSize, frameSize),
                    cornerRadius = CornerRadius(FrameRadius.toPx()),
                    style = frameStroke,
                )
            }
            if (still) return@onDrawBehind
            translate(left = lerp(SWEEP_FROM, SWEEP_TO, sweep.value) * bandWidth) {
                drawContext.canvas.skew(SWEEP_SKEW_DEGREES, 0f)
                drawRect(band, size = Size(bandWidth, size.height))
                drawContext.canvas.skew(-SWEEP_SKEW_DEGREES, 0f)
            }
        }
    }
}

/** Неподвижная карточка: рамка стоит, блика нет. */
@Composable
private fun rememberStillEffects(): Pair<State<Float>, State<Float>> =
    remember { mutableFloatStateOf(0f) to mutableFloatStateOf(0f) }

@Composable
private fun rememberRunningEffects(): Pair<State<Float>, State<Float>> {
    val transition = rememberInfiniteTransition(label = "subscription")
    val sweep = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(SWEEP_MS, easing = FastOutSlowInEasing),
            RepeatMode.Restart,
            initialStartOffset = StartOffset(SWEEP_DELAY_MS),
        ),
        label = "sweep",
    )
    val spin = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(SPIN_MS, easing = LinearEasing)),
        label = "spin",
    )
    return sweep to spin
}

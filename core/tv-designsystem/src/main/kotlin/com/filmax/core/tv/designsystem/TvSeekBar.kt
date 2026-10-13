// Полоса перемотки плеера: дорожка, загруженное, просмотренное и бегунок со свечением.
package com.filmax.core.tv.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlin.math.roundToInt

private val ThumbSize = 18.dp
private val LabelGap = 10.dp
private val LabelShape = RoundedCornerShape(6.dp)
private const val TRACK_ALPHA = 0.22f
private const val BUFFERED_ALPHA = 0.3f

/** Тёмный текст на белой плашке времени — плашка читается на любом кадре. */
private val LabelText = Color(0xFF111111)
private val LabelStyle = TextStyle(
    fontFamily = Onest,
    fontWeight = FontWeight.Bold,
    fontSize = 12.sp,
    lineHeight = 16.sp
)

/**
 * Полоса перемотки. [progress] и [buffered] — доли 0..1; читаются только при раскладке и
 * отрисовке, поэтому тик позиции раз в секунду не перекомпоновывает ни полосу, ни экран.
 *
 * [thumb] = false — тонкая полоса без бегунка (прогресс над открытой панелью). [thumbLabel] —
 * плашка времени над бегунком, пока перемотка «взята в руки»; null — без плашки. Бегунок
 * пульсирует свечением палитры — по нему видно, где сейчас позиция.
 */
// Компонент дизайн-системы: параметры — его публичный API (доли, modifier, хвост опций вида).
@Suppress("LongParameterList")
@Composable
fun TvSeekBar(
    progress: () -> Float,
    buffered: () -> Float,
    modifier: Modifier = Modifier,
    thickness: Dp = 6.dp,
    thumb: Boolean = true,
    thumbLabel: String? = null,
) {
    val accent = LocalFxColors.current.accent
    Box(modifier.fillMaxWidth().height(if (thumb) ThumbSize else thickness)) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(thickness)
                .drawBehind {
                    val radius = CornerRadius(size.height / 2)
                    drawRoundRect(Color.White.copy(alpha = TRACK_ALPHA), cornerRadius = radius)
                    drawPart(buffered(), Color.White.copy(alpha = BUFFERED_ALPHA), radius)
                    drawPart(progress(), accent, radius)
                },
        )
        if (thumb) {
            Box(
                Modifier
                    .atFraction(progress)
                    .size(ThumbSize)
                    .fxGlowPulse(active = true, cornerRadius = ThumbSize)
                    .background(Color.White, CircleShape),
            )
        }
        if (thumb && thumbLabel != null) {
            Text(
                thumbLabel,
                style = LabelStyle,
                color = LabelText,
                modifier = Modifier
                    .atFraction(progress, above = ThumbSize + LabelGap)
                    .background(Color.White, LabelShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

private fun DrawScope.drawPart(fraction: Float, color: Color, radius: CornerRadius) {
    val width = size.width * fraction.coerceIn(0f, 1f)
    if (width > 0f) drawRoundRect(color, topLeft = Offset.Zero, size = Size(width, size.height), cornerRadius = radius)
}

/**
 * Ставит элемент центром на долю [fraction] ширины родителя, не вылезая за края. [above] — поднять
 * над полосой на столько (плашка времени над бегунком). Позиция читается при размещении.
 */
private fun Modifier.atFraction(fraction: () -> Float, above: Dp = 0.dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    val width = constraints.maxWidth
    layout(width, placeable.height) {
        val center = fraction().coerceIn(0f, 1f) * width
        val left = (center - placeable.width / 2f).roundToInt().coerceIn(0, (width - placeable.width).coerceAtLeast(0))
        placeable.place(left, -above.roundToPx())
    }
}

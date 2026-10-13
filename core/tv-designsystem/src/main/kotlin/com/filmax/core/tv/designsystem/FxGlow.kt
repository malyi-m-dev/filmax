package com.filmax.core.tv.designsystem

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Свечение выходит за элемент на столько и размыто на столько (box-shadow 0 0 30px 6px). */
private val GlowSpread = 6.dp
private val GlowBlur = 18.dp
private const val PULSE_MIN_ALPHA = 0.35f

/**
 * Цветное свечение вокруг элемента в фокусе, пульсирующее alpha 0.35↔1 за 1.4 с. [cornerRadius] —
 * скругление самого элемента: свечение повторяет его форму. Без фокуса ничего не рисует.
 *
 * Размытие дорогое, поэтому свечение рисуется один раз в текстуру ([GlowImages]) и дальше только
 * выводится с прозрачностью пульса. Пульс читается при отрисовке, а не в композиции: элемент в
 * фокусе не перекомпонуется 60 раз в секунду. В облегчённом режиме и при сниженной анимации
 * свечение горит ровно — экран не перерисовывается, пока ничего не происходит.
 */
fun Modifier.fxGlowPulse(active: Boolean, cornerRadius: Dp): Modifier = composed {
    if (!active) return@composed this
    val color = LocalFxColors.current.glow
    val steady = LocalReducedMotion.current || LocalLightEffects.current
    val pulse = if (steady) remember { mutableFloatStateOf(1f) } else rememberPulse()
    drawWithCache {
        val spec = GlowSpec(
            width = size.width.roundToInt(),
            height = size.height.roundToInt(),
            cornerRadius = cornerRadius.toPx(),
            spread = GlowSpread.toPx(),
            blur = GlowBlur.toPx(),
            color = color.toArgb(),
        )
        val image = GlowImages.get(spec)
        val origin = Offset(-spec.margin, -spec.margin)
        onDrawBehind { drawImage(image, origin, alpha = pulse.value) }
    }
}

@Composable
private fun rememberPulse(): State<Float> = rememberInfiniteTransition(label = "glowPulse").animateFloat(
    initialValue = PULSE_MIN_ALPHA,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(FxMotion.PULSE_MS, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    label = "glowPulseAlpha",
)

/** Свечение конкретного размера и цвета — ключ кэша текстур. */
private data class GlowSpec(
    val width: Int,
    val height: Int,
    val cornerRadius: Float,
    val spread: Float,
    val blur: Float,
    val color: Int,
) {
    /** Насколько текстура шире элемента с каждой стороны: разлёт плюс хвост размытия. */
    val margin: Float get() = spread + blur
}

/**
 * Текстуры свечения. Карточки одного ряда и сетки одного размера, поэтому на всю ленту их
 * несколько: размытие считается один раз на размер, а не на каждый кадр каждой карточки.
 * Только главный поток — рисует Compose там же.
 */
private object GlowImages {
    private const val CAPACITY = 8

    private val cache = object : LinkedHashMap<GlowSpec, ImageBitmap>(CAPACITY, LOAD_FACTOR, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<GlowSpec, ImageBitmap>?): Boolean =
            size > CAPACITY
    }

    fun get(spec: GlowSpec): ImageBitmap = cache.getOrPut(spec) { render(spec) }

    /**
     * Размытый скруглённый прямоугольник в программном Canvas: BlurMaskFilter там работает на
     * любой версии Android, в отличие от аппаратного рендера до Android 9.
     */
    private fun render(spec: GlowSpec): ImageBitmap {
        val margin = spec.margin
        val bitmap = Bitmap.createBitmap(
            (spec.width + margin * 2).roundToInt().coerceAtLeast(1),
            (spec.height + margin * 2).roundToInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = spec.color
            maskFilter = BlurMaskFilter(spec.blur, BlurMaskFilter.Blur.NORMAL)
        }
        val inset = spec.blur
        // Круглому элементу передают заведомо большой радиус — свечение не может быть круглее круга.
        val radius = minOf(spec.cornerRadius + spec.spread, (minOf(spec.width, spec.height) / 2f) + spec.spread)
        Canvas(bitmap).drawRoundRect(inset, inset, bitmap.width - inset, bitmap.height - inset, radius, radius, paint)
        return bitmap.asImageBitmap()
    }

    private const val LOAD_FACTOR = 0.75f
}

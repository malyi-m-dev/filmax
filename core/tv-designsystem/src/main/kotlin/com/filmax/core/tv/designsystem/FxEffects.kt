// Эффекты редизайна: появление контента, ken-burns фона, пульсирующее свечение фокуса, стекло
// и прокрутка с опорной точкой. Тайминги — только из FxMotion.
@file:OptIn(ExperimentalFoundationApi::class)

package com.filmax.core.tv.designsystem

import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.delay

/** Сдвиг снизу, с которого появляется контент. */
private val EnterOffset = 16.dp

/**
 * Появление по очереди: alpha 0→1 и подъём на 16dp, каждый следующий элемент на 60 мс позже.
 * [index] — порядковый номер элемента в группе (0 — первый).
 */
fun Modifier.fxEnter(index: Int): Modifier = composed {
    val progress = remember { Animatable(0f) }
    val reduced = LocalReducedMotion.current
    LaunchedEffect(Unit) {
        if (reduced) {
            progress.snapTo(1f)
        } else {
            delay(index * FxMotion.ENTER_STAGGER_MS.toLong())
            progress.animateTo(1f, tween(FxMotion.ENTER_MS, easing = FxEasing.Standard))
        }
    }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * EnterOffset.toPx()
    }
}

private const val KEN_BURNS_SCALE_FROM = 1.03f
private const val KEN_BURNS_SCALE_RANGE = 0.11f
private const val KEN_BURNS_SHIFT_X = -0.025f
private const val KEN_BURNS_SHIFT_Y = -0.015f

/**
 * Ken-burns фона hero и деталей: медленное приближение 1.03→1.14 со сдвигом влево-вверх, 16 с
 * туда и обратно. [running] = false ставит на паузу (hero ушёл из кадра, шапка свёрнута).
 */
fun Modifier.kenBurns(running: Boolean = true): Modifier = composed {
    val reduced = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "kenBurns")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(FxMotion.KEN_BURNS_MS, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "kenBurnsPhase",
    )
    graphicsLayer {
        val progress = if (running && !reduced) phase else 0f
        val scale = KEN_BURNS_SCALE_FROM + KEN_BURNS_SCALE_RANGE * progress
        scaleX = scale
        scaleY = scale
        translationX = KEN_BURNS_SHIFT_X * size.width * progress
        translationY = KEN_BURNS_SHIFT_Y * size.height * progress
    }
}

/** Свечение выходит за элемент на столько и размыто на столько (box-shadow 0 0 30px 6px). */
private val GlowSpread = 6.dp
private val GlowBlur = 18.dp
private const val PULSE_MIN_ALPHA = 0.35f

/**
 * Цветное свечение вокруг элемента в фокусе, пульсирующее alpha 0.35↔1 за 1.4 с. [cornerRadius] —
 * скругление самого элемента: свечение повторяет его форму. Без фокуса ничего не рисует.
 */
fun Modifier.fxGlowPulse(active: Boolean, cornerRadius: Dp): Modifier = composed {
    if (!active) return@composed this
    val glow = LocalFxColors.current.glow
    val reduced = LocalReducedMotion.current
    val pulse = if (reduced) {
        1f
    } else {
        val transition = rememberInfiniteTransition(label = "glowPulse")
        val alpha by transition.animateFloat(
            initialValue = PULSE_MIN_ALPHA,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(FxMotion.PULSE_MS, easing = FastOutSlowInEasing),
                RepeatMode.Reverse
            ),
            label = "glowPulseAlpha",
        )
        alpha
    }
    drawBehind { drawGlow(glow.copy(alpha = glow.alpha * pulse), cornerRadius) }
}

/**
 * Мягкое свечение: размытый BlurMaskFilter. До Android 9 аппаратный рендер маску игнорирует и
 * нарисовал бы сплошной прямоугольник — там свечение собирается из нескольких полупрозрачных
 * обводок.
 */
private fun DrawScope.drawGlow(color: Color, cornerRadius: Dp) {
    val spread = GlowSpread.toPx()
    val topLeft = Offset(-spread, -spread)
    val glowSize = Size(size.width + spread * 2, size.height + spread * 2)
    // Круглому элементу передают заведомо большой радиус — свечение не может быть круглее круга.
    val radius = minOf(cornerRadius.toPx() + spread, glowSize.minDimension / 2)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        drawIntoCanvas { canvas ->
            val paint = Paint().apply { this.color = color }
            paint.asFrameworkPaint().maskFilter = BlurMaskFilter(GlowBlur.toPx(), BlurMaskFilter.Blur.NORMAL)
            canvas.drawRoundRect(
                left = topLeft.x,
                top = topLeft.y,
                right = topLeft.x + glowSize.width,
                bottom = topLeft.y + glowSize.height,
                radiusX = radius,
                radiusY = radius,
                paint = paint,
            )
        }
    } else {
        repeat(GLOW_FALLBACK_RINGS) { ring ->
            val inset = spread * ring / GLOW_FALLBACK_RINGS
            drawRoundRect(
                color = color.copy(alpha = color.alpha * (ring + 1) / (GLOW_FALLBACK_RINGS * 2)),
                topLeft = Offset(topLeft.x + inset, topLeft.y + inset),
                size = Size(glowSize.width - inset * 2, glowSize.height - inset * 2),
                cornerRadius = CornerRadius(radius - inset),
                style = Stroke(width = spread / GLOW_FALLBACK_RINGS * 2),
            )
        }
    }
}

private const val GLOW_FALLBACK_RINGS = 4

/**
 * Стеклянная панель: полупрозрачный белый поверх того, что под ней, и тонкая светлая обводка.
 * Размыть то, что лежит под панелью, Compose не умеет — стекло держится на прозрачности.
 */
@Composable
fun Modifier.fxGlass(shape: Shape, fill: Color = LocalFxColors.current.surface): Modifier =
    background(fill, shape).border(1.dp, LocalFxColors.current.outline, shape)

/**
 * Прокрутка к «опорной точке», как в лентах Google TV: элемент в фокусе встаёт краем на
 * [pivotPx] от начала контейнера — двигается лента, а не фокус. Высокий блок (больше двух третей
 * контейнера — hero) прижимается к самому началу: на опорной точке его верх ушёл бы за край.
 * Ряд постеров с подписями бывает выше половины экрана, поэтому порог — две трети.
 * У краёв списка Lazy-контейнер сам ограничит скролл.
 *
 * Чтобы на опорную точку вставал ряд целиком, а не карточка внутри него, ряд помечается
 * [bringIntoViewAsWhole].
 */
class PivotBringIntoViewSpec(private val pivotPx: Float) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float =
        if (size > containerSize * TALL_BLOCK_FRACTION) offset else offset - pivotPx
}

private const val TALL_BLOCK_FRACTION = 2f / 3f

/**
 * Прокрутка к элементу внутри этого блока показывает блок целиком: запрос «покажи карточку»
 * от вложенного фокуса уходит к родительскому списку как «покажи весь ряд». Так ряд встаёт на
 * опорную точку заголовком, а не серединой.
 */
fun Modifier.bringIntoViewAsWhole(): Modifier = composed {
    val responder = remember { WholeBlockResponder() }
    onSizeChanged { responder.size = it }.bringIntoViewResponder(responder)
}

private class WholeBlockResponder : BringIntoViewResponder {
    var size: IntSize = IntSize.Zero

    override fun calculateRectForParent(localRect: Rect): Rect = Rect(Offset.Zero, size.toSize())

    // Внутри блока прокручивать нечего — горизонтальный ряд уже показал карточку сам.
    override suspend fun bringChildIntoView(localRect: () -> Rect?) = Unit
}

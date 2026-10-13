package com.filmax.core.tv.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

private val FadeHeight = 110.dp

/** Сколько контента остаётся видно на середине затухания (40% высоты от края). */
private const val MIDDLE_VISIBLE = 0.2f
private const val MIDDLE_STOP = 0.4f

/**
 * Какие края списка сейчас затухают: 1 — край гаснет полностью, 0 — затухания нет. Значения
 * анимируются и читаются только при отрисовке — прокрутка не перекомпоновывает список.
 */
@Stable
class TvScrollFades internal constructor(private val top: State<Float>, private val bottom: State<Float>) {
    internal val topStrength: Float get() = top.value
    internal val bottomStrength: Float get() = bottom.value
}

/**
 * Затухание у края, за которым есть ещё элементы: список прокручен вниз — гаснет верх, ниже
 * есть ещё — низ; у края, где ничего не осталось, затухание уходит. [state] — любой
 * прокручиваемый контейнер: список, сетка.
 */
@Composable
fun rememberTvScrollFades(state: ScrollableState): TvScrollFades {
    val top = animateFloatAsState(if (state.canScrollBackward) 1f else 0f, FxMotion.fade(), label = "topFade")
    val bottom = animateFloatAsState(if (state.canScrollForward) 1f else 0f, FxMotion.fade(), label = "bottomFade")
    return remember(top, bottom) { TvScrollFades(top, bottom) }
}

/**
 * Гасит сам контент у краёв маской, а не красит поверх: под ним просвечивает то, на чём лежит
 * список (стекло панели, фон), и шва с соседними блоками нет. Маске нужен отдельный слой.
 * Чтобы последний элемент проскролливался выше затухания, списку нужен нижний отступ (~40dp).
 */
fun Modifier.tvScrollFades(fades: TvScrollFades, height: Dp = FadeHeight): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val fadePx = height.toPx().coerceAtMost(size.height / 2)
            drawEdgeMask(fades.topStrength, top = 0f, bottom = fadePx, fromEdge = true)
            drawEdgeMask(fades.bottomStrength, top = size.height - fadePx, bottom = size.height, fromEdge = false)
        }

/** Полоса маски от края внутрь: у края контент виден на `1 - strength`, к концу полосы — целиком. */
private fun DrawScope.drawEdgeMask(strength: Float, top: Float, bottom: Float, fromEdge: Boolean) {
    if (strength == 0f) return
    val edge = Color.Black.copy(alpha = 1f - strength)
    val middle = Color.Black.copy(alpha = lerp(1f, MIDDLE_VISIBLE, strength))
    val brush = if (fromEdge) {
        Brush.verticalGradient(0f to edge, MIDDLE_STOP to middle, 1f to Color.Black, startY = top, endY = bottom)
    } else {
        Brush.verticalGradient(0f to Color.Black, 1f - MIDDLE_STOP to middle, 1f to edge, startY = top, endY = bottom)
    }
    drawRect(
        brush = brush,
        topLeft = Offset(0f, top),
        size = Size(size.width, bottom - top),
        blendMode = BlendMode.DstIn,
    )
}

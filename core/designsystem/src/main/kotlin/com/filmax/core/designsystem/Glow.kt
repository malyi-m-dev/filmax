package com.filmax.core.designsystem

import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Свечение под элементом — `box-shadow: 0 10px 30px -8px glow` макета: размытая копия формы цвета
 * [color], ужатая на [spread] и сдвинутая вниз на [offsetY]. Главная кнопка, выбранная вкладка и
 * аватар «парят» над кадром, а не обведены рамкой.
 *
 * Размытие маской аппаратный холст умеет с Android 9; раньше свечения нет — элемент просто без него,
 * резкая цветная подложка вместо мягкой была бы хуже.
 */
fun Modifier.glow(
    color: Color,
    shape: Shape,
    blur: Dp = DefaultBlur,
    spread: Dp = DefaultSpread,
    offsetY: Dp = DefaultOffset,
): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || color.alpha == 0f) return this
    return drawBehind {
        val spreadPx = spread.toPx()
        val glowSize = Size(size.width + spreadPx * 2, size.height + spreadPx * 2)
        if (glowSize.width <= 0f || glowSize.height <= 0f) return@drawBehind
        val outline = shape.createOutline(glowSize, layoutDirection, this)
        val paint = Paint().apply {
            this.color = color
            asFrameworkPaint().maskFilter = BlurMaskFilter(blur.toPx() * CSS_BLUR_TO_MASK, BlurMaskFilter.Blur.NORMAL)
        }
        translate(left = -spreadPx, top = -spreadPx + offsetY.toPx()) {
            drawIntoCanvas { canvas -> canvas.drawOutline(outline, paint) }
        }
    }
}

/** Свечение главной кнопки макета: `0 10px 30px -8px`. */
private val DefaultBlur = 30.dp
private val DefaultSpread = (-8).dp
private val DefaultOffset = 10.dp

/** CSS-размытие задаёт диаметр, маска Skia — сигму: половины хватает, чтобы совпасть с макетом. */
private const val CSS_BLUR_TO_MASK = 0.5f

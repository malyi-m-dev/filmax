package com.filmax.core.designsystem

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Линейный градиент под углом, как `linear-gradient(160deg, …)` в CSS макета: 0° — снизу вверх,
 * 90° — слева направо, 180° — сверху вниз. Длина линии — как у браузера, чтобы края карточки
 * получили ровно крайние цвета, а не обрезанную середину.
 */
fun angledGradient(angle: Float, vararg stops: Pair<Float, Color>): Brush = object : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val radians = Math.toRadians(angle.toDouble())
        val dirX = sin(radians).toFloat()
        val dirY = -cos(radians).toFloat()
        val half = (abs(size.width * dirX) + abs(size.height * dirY)) / 2
        val center = Offset(size.width / 2, size.height / 2)
        return LinearGradientShader(
            from = center - Offset(dirX * half, dirY * half),
            to = center + Offset(dirX * half, dirY * half),
            colors = stops.map { it.second },
            colorStops = stops.map { it.first },
        )
    }
}

/**
 * Цветная подсветка экрана — эллипс `radial-gradient(rx ry at x y, color, transparent)` макета:
 * под hero Главной, сверху Поиска и Профиля. Центр и радиусы — доли размера элемента.
 */
fun Modifier.radialWash(
    color: Color,
    centerX: Float,
    centerY: Float,
    radiusX: Float,
    radiusY: Float,
): Modifier = drawBehind {
    val center = Offset(size.width * centerX, size.height * centerY)
    val radius = size.height * radiusY
    if (radius <= 0f) return@drawBehind
    val stretch = size.width * radiusX / radius
    scale(scaleX = stretch, scaleY = 1f, pivot = center) {
        drawCircle(
            brush = Brush.radialGradient(listOf(color, Color.Transparent), center = center, radius = radius),
            radius = radius,
            center = center,
        )
    }
}

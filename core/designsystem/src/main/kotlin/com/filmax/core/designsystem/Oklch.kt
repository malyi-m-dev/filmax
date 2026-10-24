package com.filmax.core.designsystem

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Цвет из oklch, как он записан в макете: [lightness] 0..1, [chroma] ~0..0.37, [hue] в градусах.
 * Оттенки подборок, папок и плиток разделов макет задаёт одной формулой с разным hue — считать
 * их здесь честнее, чем держать таблицу переведённых вручную hex.
 *
 * Цвет вне sRGB обрезается по каналам: так же поступает браузер, где макет и смотрели.
 */
fun oklch(lightness: Float, chroma: Float, hue: Float, alpha: Float = 1f): Color {
    val radians = Math.toRadians(hue.toDouble())
    val labA = chroma * cos(radians)
    val labB = chroma * sin(radians)
    val lms = OklabToLms.map { row -> row[0] * lightness + row[1] * labA + row[2] * labB }
        .map { it * it * it }
    val linear = LmsToLinearSrgb.map { row -> row.indices.sumOf { row[it] * lms[it] } }
    val (red, green, blue) = linear.map { encodeSrgb(it).coerceIn(0.0, 1.0).toFloat() }
    return Color(red, green, blue, alpha.coerceIn(0f, 1f))
}

/** Гамма sRGB: линейная яркость → значение канала. */
private fun encodeSrgb(linear: Double): Double =
    if (linear <= LINEAR_THRESHOLD) linear * LINEAR_SLOPE else GAMMA_SCALE * linear.pow(1 / GAMMA) - GAMMA_OFFSET

// Матрицы OKLab (Björn Ottosson, 2020): L·a·b → корни LMS, затем LMS → линейный sRGB.
private val OklabToLms = listOf(
    doubleArrayOf(1.0, 0.3963377774, 0.2158037573),
    doubleArrayOf(1.0, -0.1055613458, -0.0638541728),
    doubleArrayOf(1.0, -0.0894841775, -1.2914855480),
)
private val LmsToLinearSrgb = listOf(
    doubleArrayOf(4.0767416621, -3.3077115913, 0.2309699292),
    doubleArrayOf(-1.2684380046, 2.6097574011, -0.3413193965),
    doubleArrayOf(-0.0041960863, -0.7034186147, 1.7076147010),
)

private const val LINEAR_THRESHOLD = 0.0031308
private const val LINEAR_SLOPE = 12.92
private const val GAMMA = 2.4
private const val GAMMA_SCALE = 1.055
private const val GAMMA_OFFSET = 0.055

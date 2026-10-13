package com.filmax.core.tv.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp

/**
 * Подсветка — полоса 260dp по верху экрана: эллипс с центром у верхнего края на 85% ширины,
 * полуось по горизонтали — половина ширины, по вертикали — вся полоса; к 75% радиуса гаснет.
 */
private val WashHeight = 260.dp
private const val WASH_CENTER_X = 0.85f
private const val WASH_RADIUS_X = 0.5f
private const val WASH_FADE_STOP = 0.75f

/**
 * Фон разделов с меню («Моё», «Профиль»): мягкая подсветка ([FxColors.wash]) из правого верхнего
 * угла — экран без кадра не выглядит плоским, а смена палитры видна сразу. Сплошной заливки нет:
 * фон — у окна ([FilmaxTvTheme]), второй слой того же цвета только закрашивал бы экран дважды.
 */
@Composable
fun Modifier.tvSectionBackground(): Modifier {
    val wash = LocalFxColors.current.wash
    return drawWithCache {
        val radiusX = size.width * WASH_RADIUS_X
        val bandHeight = WashHeight.toPx().coerceAtMost(size.height)
        val center = Offset(size.width * WASH_CENTER_X, 0f)
        val brush = Brush.radialGradient(
            0f to wash,
            WASH_FADE_STOP to Color.Transparent,
            center = center,
            radius = radiusX,
        )
        // Эллипс — круговой градиент, сжатый по вертикали до высоты полосы.
        val squeeze = bandHeight / radiusX
        onDrawBehind {
            scale(scaleX = 1f, scaleY = squeeze, pivot = center) {
                drawRect(brush, size = Size(size.width, radiusX))
            }
        }
    }
}

/**
 * Фон экрана, окрашенного предметом (папка): градиент цвета [tint] сверху в фон палитры на высоту
 * шапки. [height] — доля высоты экрана, которую занимает переход. Ниже — фон окна.
 */
@Composable
fun Modifier.tvTintedBackground(tint: Color, height: Float): Modifier {
    val background = LocalFxColors.current.background
    return drawWithCache {
        val bandHeight = size.height * height
        val brush = Brush.verticalGradient(colors = listOf(tint, background), endY = bandHeight)
        onDrawBehind { drawRect(brush, size = size.copy(height = bandHeight)) }
    }
}

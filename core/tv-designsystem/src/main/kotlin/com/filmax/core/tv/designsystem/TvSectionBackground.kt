package com.filmax.core.tv.designsystem

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Подсветка занимает верх экрана: центр — у правого края на 85% ширины, радиус — половина ширины. */
private const val WASH_CENTER_X = 0.85f
private const val WASH_RADIUS = 0.5f

/**
 * Фон разделов с меню («Моё», «Профиль»): цвет фона палитры и мягкая подсветка ([FxColors.wash])
 * из правого верхнего угла — экран без кадра не выглядит плоским, а смена палитры видна сразу.
 */
@Composable
fun Modifier.tvSectionBackground(): Modifier {
    val colors = LocalFxColors.current
    return background(colors.background).drawBehind {
        drawRect(
            Brush.radialGradient(
                colors = listOf(colors.wash, Color.Transparent),
                center = Offset(size.width * WASH_CENTER_X, 0f),
                radius = size.width * WASH_RADIUS,
            ),
        )
    }
}

/**
 * Фон экрана, окрашенного предметом (папка): градиент цвета [tint] сверху в фон палитры на высоту
 * шапки. [height] — доля высоты экрана, которую занимает переход.
 */
@Composable
fun Modifier.tvTintedBackground(tint: Color, height: Float): Modifier {
    val background = LocalFxColors.current.background
    return background(background).drawBehind {
        drawRect(
            Brush.verticalGradient(
                colors = listOf(tint, background),
                endY = size.height * height,
            ),
            size = size.copy(height = size.height * height),
        )
    }
}

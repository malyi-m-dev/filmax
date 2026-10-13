package com.filmax.core.tv.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import com.filmax.core.ui.components.PosterImage

/** Подсветка палитры — эллипс в правом верхнем углу (radial-gradient 60% 80% at 100% 0%). */
private const val WASH_RADIUS = 0.6f

/** Затемнение слева — глухое до 10% ширины, прозрачное с 75%; снизу — до 18% высоты, прозрачное с 62%. */
private const val LEFT_SOLID = 0.1f
private const val LEFT_CLEAR = 0.75f
private const val BOTTOM_SOLID = 0.82f
private const val BOTTOM_CLEAR = 0.38f

/**
 * Фон экрана тайтла — кадр на весь экран с ken-burns, подсветкой палитры и затемнением слева и
 * снизу под текст. [dim] (0..1) — дополнительное затемнение поверх: растёт с прокруткой, чтобы
 * ряды ниже первого экрана читались на любом кадре. [running] = false ставит ken-burns на паузу.
 */
@Composable
fun TvBackdrop(imageUrl: String, modifier: Modifier = Modifier, dim: () -> Float = { 0f }, running: Boolean = true) {
    val colors = LocalFxColors.current
    Box(
        modifier
            .fillMaxSize()
            .clipToBounds()
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(colors.wash, Color.Transparent),
                        center = Offset(size.width, 0f),
                        radius = size.width * WASH_RADIUS,
                    ),
                )
                drawRect(Brush.horizontalGradient(LEFT_SOLID to colors.background, LEFT_CLEAR to Color.Transparent))
                drawRect(Brush.verticalGradient(BOTTOM_CLEAR to Color.Transparent, BOTTOM_SOLID to colors.background))
                drawRect(colors.background.copy(alpha = dim().coerceIn(0f, 1f)))
            },
    ) {
        PosterImage(
            url = imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().kenBurns(running),
            shape = RectangleShape,
            accentColor = TvSurfaceContainerHigh,
        )
    }
}

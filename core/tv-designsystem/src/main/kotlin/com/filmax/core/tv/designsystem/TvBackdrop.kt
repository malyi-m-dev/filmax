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
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.filmax.core.ui.components.PosterImage

/**
 * Раскладка затемнений фона под содержимое экрана тайтла.
 *
 * - [Title] — фильм: текст слева, ряды ниже первого экрана. Подсветка в правом верхнем углу,
 *   затемнение слева и плотное снизу — под «В ролях».
 * - [Split] — сериал: информация слева, панель «Эпизоды» справа. Подсветка ближе к центру — над
 *   колонкой текста, правый край притемнён под стекло панели, снизу затемнение только у кромки.
 */
enum class TvBackdropStyle { Title, Split }

/** Подсветка палитры (radial-gradient 60% … at X% 0%): центр по ширине и радиус в долях ширины. */
private const val WASH_RADIUS = 0.6f
private const val TITLE_WASH_X = 1f
private const val SPLIT_WASH_X = 0.4f

/** Фильм: слева глухо до 10% ширины, прозрачно с 75%; снизу — глухо от 18% высоты, прозрачно с 62%. */
private const val TITLE_LEFT_SOLID = 0.1f
private const val TITLE_LEFT_CLEAR = 0.75f
private const val TITLE_BOTTOM_SOLID = 0.82f
private const val TITLE_BOTTOM_CLEAR = 0.38f

/** Сериал: слева глухо до 12%, прозрачно с 60%, к правому краю — тень 0.55 под панель; снизу — от 4%. */
private const val SPLIT_LEFT_SOLID = 0.12f
private const val SPLIT_LEFT_CLEAR = 0.6f
private const val SPLIT_RIGHT_SHADE = 0.55f
private const val SPLIT_BOTTOM_SOLID = 0.96f
private const val SPLIT_BOTTOM_CLEAR = 0.4f

/**
 * Фон экрана тайтла — кадр на весь экран с ken-burns, подсветкой палитры и затемнениями под
 * содержимое ([style]). [dim] (0..1) — дополнительное затемнение поверх: растёт с прокруткой,
 * чтобы ряды ниже первого экрана читались на любом кадре. [running] = false ставит ken-burns на паузу.
 */
@Composable
fun TvBackdrop(
    imageUrl: String,
    modifier: Modifier = Modifier,
    style: TvBackdropStyle = TvBackdropStyle.Title,
    dim: () -> Float = { 0f },
    running: Boolean = true,
) {
    val colors = LocalFxColors.current
    Box(
        modifier
            .fillMaxSize()
            .clipToBounds()
            // Затемнение прокрутки меняется с каждым шагом — отдельным проходом и только когда оно есть.
            .drawWithContent {
                drawContent()
                val alpha = dim().coerceIn(0f, 1f)
                if (alpha > 0f) drawRect(colors.background.copy(alpha = alpha))
            }
            // Неподвижные подсветка и затемнения — готовой текстурой, а не тремя градиентами на кадр.
            .fxStaticOverlay {
                when (style) {
                    TvBackdropStyle.Title -> drawTitleScrims(colors)
                    TvBackdropStyle.Split -> drawSplitScrims(colors)
                }
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

private fun DrawScope.drawTitleScrims(colors: FxColors) {
    drawWash(colors, TITLE_WASH_X)
    drawRect(Brush.horizontalGradient(TITLE_LEFT_SOLID to colors.background, TITLE_LEFT_CLEAR to Color.Transparent))
    drawRect(Brush.verticalGradient(TITLE_BOTTOM_CLEAR to Color.Transparent, TITLE_BOTTOM_SOLID to colors.background))
}

private fun DrawScope.drawSplitScrims(colors: FxColors) {
    drawWash(colors, SPLIT_WASH_X)
    drawRect(
        Brush.horizontalGradient(
            SPLIT_LEFT_SOLID to colors.background,
            SPLIT_LEFT_CLEAR to Color.Transparent,
            1f to Color.Black.copy(alpha = SPLIT_RIGHT_SHADE),
        ),
    )
    drawRect(Brush.verticalGradient(SPLIT_BOTTOM_CLEAR to Color.Transparent, SPLIT_BOTTOM_SOLID to colors.background))
}

/** Подсветка палитры — эллипс у верхнего края с центром на доле [centerX] ширины. */
private fun DrawScope.drawWash(colors: FxColors, centerX: Float) {
    drawRect(
        Brush.radialGradient(
            colors = listOf(colors.wash, Color.Transparent),
            center = Offset(size.width * centerX, 0f),
            radius = size.width * WASH_RADIUS,
        ),
    )
}

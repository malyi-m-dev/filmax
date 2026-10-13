package com.filmax.core.tv.designsystem

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Размытие экрана под панелью; голосовому оверлею макет даёт 6dp. */
val FxBackdropBlur = 8.dp

private const val BACKDROP_DIM = 0.62f
private const val BACKDROP_FADE_MS = 300

/** Цвет затемнения — глубже фона палитры, чтобы панель или оверлей отделялись от экрана. */
private val BackdropColor = Color(0xFF06050C)

/**
 * Экран под панелью или оверлеем: пока [active], он размыт на [blurRadius] (Android 12+, ниже и в
 * облегчённом режиме — только затемнение) и затемнён до 0.62; переход — 300 мс в обе стороны.
 *
 * Размытие и затемнение читаются при отрисовке: анимация не перекомпонует экран под панелью.
 * Бесконечные анимации этого экрана ставит на паузу тот, кто его накрывает ([LocalFxPaused]).
 */
fun Modifier.fxBackdrop(active: Boolean, blurRadius: Dp = FxBackdropBlur): Modifier = composed {
    val radius = if (LocalLightEffects.current) 0.dp else blurRadius
    val blur = animateDpAsState(if (active) radius else 0.dp, tween(BACKDROP_FADE_MS), label = "backdropBlur")
    val dim = animateFloatAsState(if (active) BACKDROP_DIM else 0f, tween(BACKDROP_FADE_MS), label = "backdropDim")
    graphicsLayer {
        val blurPx = blur.value.toPx()
        renderEffect = if (blurPx > 0f) BlurEffect(blurPx, blurPx, TileMode.Decal) else null
    }.drawWithContent {
        drawContent()
        drawRect(BackdropColor.copy(alpha = dim.value))
    }
}

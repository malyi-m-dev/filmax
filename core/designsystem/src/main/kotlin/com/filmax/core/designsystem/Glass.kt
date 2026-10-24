package com.filmax.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * Что размывает стекло: слой экрана, помеченный [glassSource]. Панель ([GlassPanel]) рисует
 * этот слой у себя под собой размытым — шапка над лентой, навигация над экраном.
 */
@Stable
class GlassState internal constructor(internal val haze: HazeState)

@Composable
fun rememberGlassState(): GlassState {
    val haze = rememberHazeState()
    return remember(haze) { GlassState(haze) }
}

/** Лента, кадр или весь экран под стеклянными панелями. */
fun Modifier.glassSource(state: GlassState): Modifier = hazeSource(state.haze)

/**
 * Плотность стекла. [Bar] — шапка, навигация, плавающая панель деталей: фон палитры на 52%, под
 * ним размытый контент. [Sheet] — шторка и боковая панель плеера: плотнее, текст на ней длинный.
 */
enum class GlassLevel(internal val tint: Float, internal val blur: Dp, internal val fallback: Float) {
    Bar(tint = 0.52f, blur = 24.dp, fallback = 0.86f),
    Sheet(tint = 0.68f, blur = 40.dp, fallback = 0.94f),
}

/**
 * Стеклянная панель: размытый [state] под полупрозрачным фоном палитры и тонкая светлая обводка.
 * Без [state] (или там, где размытия нет — до Android 12) панель просто плотнее: текст на ней
 * читается одинаково, пропадает только «матовость».
 */
// Компонент дизайн-системы: параметры — его публичный API (Compose-конвенция: modifier прямым
// параметром, хвост — опции с дефолтами). Обёртка в data-класс сломала бы «минимальный API».
@Suppress("LongParameterList")
@Composable
fun GlassPanel(
    state: GlassState?,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    level: GlassLevel = GlassLevel.Bar,
    border: Color = FilmaxColors.PanelBorder,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    val background = FilmaxTheme.palette.background
    val style = remember(background, level) {
        HazeStyle(
            backgroundColor = background,
            tint = HazeTint(background.copy(alpha = level.tint)),
            blurRadius = level.blur,
            noiseFactor = 0f,
            fallbackTint = HazeTint(background.copy(alpha = level.fallback)),
        )
    }
    val glass = if (state != null) {
        Modifier.hazeEffect(state.haze, style)
    } else {
        Modifier.background(background.copy(alpha = level.fallback))
    }
    Box(
        modifier = modifier
            .clip(shape)
            .then(glass)
            .border(width = 1.dp, color = border, shape = shape),
        contentAlignment = contentAlignment,
        content = content,
    )
}

/**
 * Мелкое стекло — круглая кнопка, чип поверх кадра или ленты: белая дымка с обводкой. Без
 * размытия: под мелким элементом оно незаметно, а кадр и так виден сквозь дымку.
 */
fun Modifier.glassFill(shape: Shape, fill: Color = FilmaxColors.Glass): Modifier =
    clip(shape).background(fill).border(width = 1.dp, color = FilmaxColors.GlassBorder, shape = shape)

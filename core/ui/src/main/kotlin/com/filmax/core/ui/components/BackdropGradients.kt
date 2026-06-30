package com.filmax.core.ui.components

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Палитра и фабрики backdrop-градиентов деталей. Раньше эти цвета и градиенты были
 * захардкожены и продублированы в mobile- и tv-экранах деталей; собраны здесь, чтобы
 * существовал единственный источник правды по цветам затемнения бэкдропа.
 *
 * Цвета затемнения зависят от темы (уходят в [Color] поверхности), поэтому фабрики
 * принимают `surface` параметром — он читается на стороне вызова из `MaterialTheme`.
 */
object BackdropGradients {

    /** Акцент постера-заглушки бэкдропа (тот же, что у обоих экранов деталей). */
    val Accent: Color = Color(0xFFB4305A)

    /** Верхний тёмный оттенок вертикального градиента бэкдропа. */
    private val ScrimTop: Color = Color(0xFF141012)

    /** Цвет затемнения-скрима, нарастающего при сворачивании героя (mobile). */
    private val ScrimDark: Color = Color(0xFF0A0809)

    /**
     * Mobile: вертикальный градиент — лёгкое затемнение сверху и плавный уход в [surface] снизу.
     * Прозрачный в середине, чтобы под ним был виден сам постер.
     */
    fun mobileVertical(surface: Color): Brush = Brush.verticalGradient(
        0f to ScrimTop.copy(alpha = MOBILE_SCRIM_TOP_ALPHA),
        MOBILE_TRANSPARENT_START to Color.Transparent,
        MOBILE_TRANSPARENT_END to Color.Transparent,
        1f to surface,
    )

    /** TV: горизонтальный градиент — затемнение в [surface] слева, прозрачность справа. */
    fun tvHorizontal(surface: Color): Brush = Brush.horizontalGradient(
        0f to surface,
        TV_HORIZONTAL_MID to surface.copy(alpha = TV_HORIZONTAL_MID_ALPHA),
        TV_HORIZONTAL_FADE_END to Color.Transparent,
    )

    /** TV: вертикальный градиент — нижний уход в [surface]. */
    fun tvVerticalBottom(surface: Color): Brush = Brush.verticalGradient(
        TV_VERTICAL_TRANSPARENT_END to Color.Transparent,
        1f to surface,
    )

    /**
     * Mobile: цвет затемнения-скрима, нарастающего по прогрессу сворачивания [progress] (0..1).
     * Накладывается отдельным слоем поверх [HeroBackdrop] на стороне mobile-экрана.
     */
    fun collapseScrim(progress: Float): Color = ScrimDark.copy(alpha = progress * COLLAPSE_SCRIM_MAX_ALPHA)

    // Точки-стопы и альфы градиентов вынесены в именованные константы (detekt MagicNumber);
    // значения не менялись — раскладка идентична прежней.
    private const val MOBILE_SCRIM_TOP_ALPHA = 0.3f
    private const val MOBILE_TRANSPARENT_START = 0.3f
    private const val MOBILE_TRANSPARENT_END = 0.7f
    private const val TV_HORIZONTAL_MID = 0.5f
    private const val TV_HORIZONTAL_MID_ALPHA = 0.6f
    private const val TV_HORIZONTAL_FADE_END = 0.85f
    private const val TV_VERTICAL_TRANSPARENT_END = 0.45f
    private const val COLLAPSE_SCRIM_MAX_ALPHA = 0.55f
}

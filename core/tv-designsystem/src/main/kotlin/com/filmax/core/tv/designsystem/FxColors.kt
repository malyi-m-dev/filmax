package com.filmax.core.tv.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Палитры редизайна «Filmax TV» ────────────────────────────────────────────────
// Яркий акцент вместо монохрома: главное действие, текущий раздел, прогресс и свечение фокуса —
// цветом палитры, рейтинги и аватар — вторым акцентом. Поверхности — полупрозрачный белый поверх
// фона: стеклянные панели лежат на кадре, а не перекрывают его. Значения — перевод из oklch
// макета (docs/tv-redesign.md), на панели ТВ их стоит сверять глазами.

/** Основной текст: тёплый белый, не #FFF — чистый белый «звенит» на тёмном фоне. */
private val FxText = Color(0xFFF4F2FA)

/** Ошибки и деструктив — тёплый красный, общий для всех палитр. */
private val FxError = Color(0xFFF07A6A)

/** Вторичный и третичный текст — тот же белый с прозрачностью: на любом фоне он одного тона. */
private const val SECONDARY_ALPHA = 0.72f
private const val DIM_ALPHA = 0.5f

/** Стекло: доля белого поверх того, что под панелью. */
private const val SURFACE_ALPHA = 0.07f
private const val SURFACE_HIGH_ALPHA = 0.12f
private const val OUTLINE_ALPHA = 0.08f

/**
 * Цвета экрана. Палитра выбирается целиком: акценты и фон подобраны друг к другу, текст и
 * поверхности общие для всех трёх.
 */
@Immutable
data class FxColors(
    /** Главное действие, текущий раздел, выбранный чип, полоса прогресса. */
    val accent: Color,
    val onAccent: Color,
    /** Второй акцент — рейтинги и аватар. */
    val accent2: Color,
    val onAccent2: Color,
    /** Акцент для текста на тёмном: надзаголовки, подсветка совпадений, выбранный вариант. */
    val accentText: Color,
    /** Свечение фокуса — акцент с прозрачностью (альфа в старшем байте ARGB). */
    val glow: Color,
    /** Цветная подсветка сверху экрана. */
    val wash: Color,
    val background: Color,
    val text: Color = FxText,
    val textSecondary: Color = FxText.copy(alpha = SECONDARY_ALPHA),
    val textDim: Color = FxText.copy(alpha = DIM_ALPHA),
    /** Стекло: панели, кнопки второго плана, поля. */
    val surface: Color = Color.White.copy(alpha = SURFACE_ALPHA),
    val surfaceHigh: Color = Color.White.copy(alpha = SURFACE_HIGH_ALPHA),
    /** Тонкая обводка стеклянных панелей. */
    val outline: Color = Color.White.copy(alpha = OUTLINE_ALPHA),
    val error: Color = FxError,
)

/** «Фиолет» — палитра по умолчанию. */
val Violet = FxColors(
    accent = Color(0xFF7B5CF5),
    onAccent = Color.White,
    accent2 = Color(0xFFE14BA0),
    onAccent2 = Color.White,
    accentText = Color(0xFFBEB2FF),
    glow = Color(0xA67B5CF5),
    wash = Color(0x669B4FD8),
    background = Color(0xFF0F0C1A),
)

/** «Коралл». */
val Coral = FxColors(
    accent = Color(0xFFF26B4C),
    onAccent = Color(0xFF1A0B06),
    accent2 = Color(0xFFF0B53C),
    onAccent2 = Color(0xFF1A1206),
    accentText = Color(0xFFFFB59E),
    glow = Color(0x99F26B4C),
    wash = Color(0x61E0603C),
    background = Color(0xFF170F0C),
)

/** «Мята». */
val Mint = FxColors(
    accent = Color(0xFF3FDBB2),
    onAccent = Color(0xFF04140F),
    accent2 = Color(0xFF4D98EE),
    onAccent2 = Color.White,
    accentText = Color(0xFF93F0D2),
    glow = Color(0x8C3FDBB2),
    wash = Color(0x663D8FC7),
    background = Color(0xFF0A1218),
)

/** Палитра текущего экрана. Ставит [FilmaxTvTheme]. */
val LocalFxColors = staticCompositionLocalOf { Violet }

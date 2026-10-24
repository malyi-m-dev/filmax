package com.filmax.core.tv.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxPalette
import com.filmax.core.designsystem.FilmaxPalettes

// ── Палитры редизайна «Filmax TV» ────────────────────────────────────────────────
// Яркий акцент вместо монохрома: главное действие, текущий раздел, прогресс и свечение фокуса —
// цветом палитры, рейтинги и аватар — вторым акцентом. Поверхности — полупрозрачный белый поверх
// фона: стеклянные панели лежат на кадре, а не перекрывают его. Сами акценты и фон — общие с
// телефоном (FilmaxPalettes), на панели ТВ их стоит сверять глазами.

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
    val text: Color = FilmaxColors.Text,
    val textSecondary: Color = FilmaxColors.TextSecondary,
    val textDim: Color = FilmaxColors.TextDim,
    /** Стекло: панели, кнопки второго плана, поля. */
    val surface: Color = Color.White.copy(alpha = SURFACE_ALPHA),
    val surfaceHigh: Color = Color.White.copy(alpha = SURFACE_HIGH_ALPHA),
    /** Тонкая обводка стеклянных панелей. */
    val outline: Color = Color.White.copy(alpha = OUTLINE_ALPHA),
    val error: Color = FilmaxColors.Error,
)

/** Цвета ТВ на общей палитре: акценты и фон — из неё, стекло и текст — общие. */
private fun FilmaxPalette.toFxColors() = FxColors(
    accent = accent,
    onAccent = onAccent,
    accent2 = accent2,
    onAccent2 = onAccent2,
    accentText = accentText,
    glow = glow,
    wash = wash,
    background = background,
)

/** «Фиолет» — палитра по умолчанию. */
val Violet = FilmaxPalettes.Violet.toFxColors()

/** «Коралл». */
val Coral = FilmaxPalettes.Coral.toFxColors()

/** «Мята». */
val Mint = FilmaxPalettes.Mint.toFxColors()

/** Палитра текущего экрана. Ставит [FilmaxTvTheme]. */
val LocalFxColors = staticCompositionLocalOf { Violet }

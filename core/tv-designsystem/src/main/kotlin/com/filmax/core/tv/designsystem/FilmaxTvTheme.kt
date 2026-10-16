@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.filmax.core.tv.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.compose.material3.MaterialTheme as ComposeMaterialTheme
import androidx.tv.material3.MaterialTheme as TvMaterialTheme
import androidx.tv.material3.darkColorScheme as tvDarkColorScheme

/**
 * TV-тема Filmax: палитра редизайна (по умолчанию «Фиолет», [FxColors]), шрифты [FxType] и
 * флаги сниженной анимации [LocalReducedMotion] и облегчённых эффектов [LocalLightEffects].
 *
 * Оборачивает контент в ДВЕ темы:
 *  - `androidx.tv.material3.MaterialTheme` — для TV-компонентов (Surface/Button/Text)
 *    с нативным D-pad фокусом из коробки;
 *  - `androidx.compose.material3.MaterialTheme` — для компонентов, которых нет в tv-material3
 *    (например, `CircularProgressIndicator`), чтобы и они брали токены темы.
 *
 * Схема своя, а не производная от мобильной: у телефона и телевизора разные поверхности.
 *
 * `primary` — акцент палитры [colors]: цвет главного действия. `border` = [TvFocus], поэтому
 * стандартная обводка фокуса у TV-Surface — белая. Палитру выбирают в Профиле; смена [colors]
 * перекрашивает весь ТВ-UI сразу — через [LocalFxColors] и Material-схемы. [plainFocus] — там же,
 * в «Настройках UI»: фокус без белой обводки и свечения ([LocalTvPlainFocus]).
 */
@Composable
fun FilmaxTvTheme(colors: FxColors = Violet, plainFocus: Boolean = false, content: @Composable () -> Unit) {
    val composeScheme = darkColorScheme(
        primary = colors.accent,
        onPrimary = colors.onAccent,
        primaryContainer = TvSurfaceContainerHigh,
        onPrimaryContainer = TvOnSurface,
        surface = colors.background,
        onSurface = TvOnSurface,
        background = colors.background,
        onBackground = TvOnSurface,
        surfaceContainer = TvSurfaceContainer,
        surfaceContainerHigh = TvSurfaceContainerHigh,
        surfaceContainerHighest = TvSurfaceContainerHighest,
        onSurfaceVariant = TvOnSurfaceVariant,
        outline = TvSurfaceContainerHighest,
        outlineVariant = TvOutlineVariant,
        error = TvError,
        errorContainer = TvErrorContainer,
    )

    val tvScheme = tvDarkColorScheme(
        primary = colors.accent,
        onPrimary = colors.onAccent,
        primaryContainer = TvSurfaceContainerHigh,
        onPrimaryContainer = TvOnSurface,
        surface = colors.background,
        onSurface = TvOnSurface,
        surfaceVariant = TvSurfaceContainerHigh,
        onSurfaceVariant = TvOnSurfaceVariant,
        background = colors.background,
        onBackground = TvOnSurface,
        border = TvFocus,
        error = TvError,
        errorContainer = TvErrorContainer,
    )

    ComposeMaterialTheme(
        colorScheme = composeScheme,
        typography = FilmaxTvTypography,
        shapes = FilmaxTvShapes,
    ) {
        TvMaterialTheme(colorScheme = tvScheme) {
            CompositionLocalProvider(
                LocalFxColors provides colors,
                LocalReducedMotion provides rememberReducedMotion(),
                LocalLightEffects provides rememberLightEffects(),
                LocalTvPlainFocus provides plainFocus,
            ) {
                FxWindowBackground(LocalFxColors.current.background)
                content()
            }
        }
    }
}

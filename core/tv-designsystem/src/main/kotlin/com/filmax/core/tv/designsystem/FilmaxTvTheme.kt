@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.filmax.core.tv.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme as TvMaterialTheme
import androidx.tv.material3.darkColorScheme as tvDarkColorScheme
import androidx.compose.material3.MaterialTheme as ComposeMaterialTheme
import com.filmax.core.designsystem.FilmaxDarkColorScheme
import com.filmax.core.designsystem.FilmaxShapes
import com.filmax.core.designsystem.FilmaxTypography

/**
 * TV-тема Filmax. Оборачивает контент в ДВЕ темы:
 *  - `androidx.tv.material3.MaterialTheme` — для TV-компонентов (Surface/Button/Text)
 *    с нативным D-pad фокусом из коробки;
 *  - `androidx.compose.material3.MaterialTheme` — для компонентов, которых нет в tv-material3
 *    (например, `CircularProgressIndicator`), чтобы и они брали брендовые цвета.
 *
 * Обе схемы построены на токенах мобильной [FilmaxDarkColorScheme] с затемнёнными
 * под 10-foot поверхностями. `border` в TV-схеме = [TvFocus], поэтому стандартная
 * обводка фокуса у TV-Surface получается фирменно-жёлтой.
 */
@Composable
fun FilmaxTvTheme(content: @Composable () -> Unit) {
    val composeScheme = FilmaxDarkColorScheme.copy(
        surface = TvSurface,
        background = TvSurface,
        surfaceContainer = TvSurfaceContainer,
        surfaceContainerHigh = TvSurfaceContainerHigh,
        surfaceContainerHighest = TvSurfaceContainerHighest,
        onSurfaceVariant = TvOnSurfaceVariant,
        outlineVariant = TvOutlineVariant,
    )

    val tvScheme = tvDarkColorScheme(
        primary = TvPrimary,
        onPrimary = TvOnPrimary,
        primaryContainer = TvPrimaryContainer,
        onPrimaryContainer = TvOnPrimaryContainer,
        surface = TvSurface,
        onSurface = TvOnSurface,
        surfaceVariant = TvSurfaceContainerHigh,
        onSurfaceVariant = TvOnSurfaceVariant,
        background = TvSurface,
        onBackground = TvOnSurface,
        border = TvFocus,
        error = TvError,
        errorContainer = TvErrorContainer,
    )

    CompositionLocalProvider(LocalTvFocusColor provides TvFocus) {
        ComposeMaterialTheme(
            colorScheme = composeScheme,
            typography = FilmaxTypography,
            shapes = FilmaxShapes,
        ) {
            TvMaterialTheme(colorScheme = tvScheme) {
                content()
            }
        }
    }
}

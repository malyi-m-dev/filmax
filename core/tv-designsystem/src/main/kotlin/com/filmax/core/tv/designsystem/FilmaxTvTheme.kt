@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.filmax.core.tv.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.compose.material3.MaterialTheme as ComposeMaterialTheme
import androidx.tv.material3.MaterialTheme as TvMaterialTheme
import androidx.tv.material3.darkColorScheme as tvDarkColorScheme

/**
 * TV-тема Filmax. Строгий монохром: единственный цвет на экране — постер.
 *
 * Оборачивает контент в ДВЕ темы:
 *  - `androidx.tv.material3.MaterialTheme` — для TV-компонентов (Surface/Button/Text)
 *    с нативным D-pad фокусом из коробки;
 *  - `androidx.compose.material3.MaterialTheme` — для компонентов, которых нет в tv-material3
 *    (например, `CircularProgressIndicator`), чтобы и они брали токены темы.
 *
 * Схема своя, а не производная от мобильной: у телефона и телевизора разные поверхности
 * (TV темнее) и разный вторичный текст (TV светлее — дистанция 3 метра съедает контраст).
 *
 * `primary` = [TvAccent] (белый): в монохроме роль «главного действия» несёт белая заливка.
 * `border` = [TvFocus], поэтому стандартная обводка фокуса у TV-Surface — белая.
 */
@Composable
fun FilmaxTvTheme(content: @Composable () -> Unit) {
    val composeScheme = darkColorScheme(
        primary = TvAccent,
        onPrimary = TvOnAccent,
        primaryContainer = TvSurfaceContainerHigh,
        onPrimaryContainer = TvOnSurface,
        surface = TvSurface,
        onSurface = TvOnSurface,
        background = TvSurface,
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
        primary = TvAccent,
        onPrimary = TvOnAccent,
        primaryContainer = TvSurfaceContainerHigh,
        onPrimaryContainer = TvOnSurface,
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

    ComposeMaterialTheme(
        colorScheme = composeScheme,
        typography = FilmaxTvTypography,
        shapes = FilmaxTvShapes,
    ) {
        TvMaterialTheme(colorScheme = tvScheme) {
            content()
        }
    }
}

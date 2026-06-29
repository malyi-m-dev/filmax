package com.filmax.core.tv.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.filmax.core.designsystem.FilmaxDarkColorScheme
import com.filmax.core.designsystem.FilmaxShapes
import com.filmax.core.designsystem.FilmaxTypography

/**
 * TV-вариант темы Filmax.
 *
 * Переиспользует токены мобильной [FilmaxDarkColorScheme] (тот же бренд: rose `#B4305A`,
 * `onSurface #EFDFE3`, …), затемняя поверхности под 10-foot просмотр и добавляя focus-цвет
 * [TvFocus] через [LocalTvFocusColor]. Типографика и формы — общие с мобильной системой.
 */
@Composable
fun FilmaxTvTheme(content: @Composable () -> Unit) {
    val colorScheme = FilmaxDarkColorScheme.copy(
        surface = TvSurface,
        background = TvSurface,
        surfaceContainer = TvSurfaceContainer,
        surfaceContainerHigh = TvSurfaceContainerHigh,
        surfaceContainerHighest = TvSurfaceContainerHighest,
        onSurfaceVariant = TvOnSurfaceVariant,
        outlineVariant = TvOutlineVariant,
    )
    CompositionLocalProvider(LocalTvFocusColor provides TvFocus) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FilmaxTypography,
            shapes = FilmaxShapes,
            content = content,
        )
    }
}

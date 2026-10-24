package com.filmax.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/** Палитра текущего экрана. Ставит [FilmaxTheme]. */
val LocalFilmaxPalette = staticCompositionLocalOf { FilmaxPalettes.Violet }

/**
 * Тема мобильного Filmax: палитра, выбранная в Профиле ([FilmaxPalettes]), стекло поверх фона
 * палитры и шкала [FilmaxType]. Смена [palette] перекрашивает весь UI сразу — через Material-схему
 * и [LocalFilmaxPalette] для того, чего в схеме нет (второй акцент, свечение, подсветка).
 */
@Composable
fun FilmaxTheme(palette: FilmaxPalette = FilmaxPalettes.Violet, content: @Composable () -> Unit) {
    val colorScheme = remember(palette) { filmaxColorScheme(palette) }
    CompositionLocalProvider(LocalFilmaxPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FilmaxTypography,
            shapes = FilmaxShapes,
            content = content,
        )
    }
}

/** Доступ к палитре темы: `FilmaxTheme.palette.accent2`. */
object FilmaxTheme {
    val palette: FilmaxPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalFilmaxPalette.current
}

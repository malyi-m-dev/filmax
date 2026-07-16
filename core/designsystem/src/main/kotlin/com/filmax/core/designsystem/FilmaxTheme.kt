package com.filmax.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Тема мобильного Filmax. Строгий монохром: единственный цвет на экране — постер.
 *
 * Параметра акцента больше нет. Шесть цветных пресетов убраны вместе с редизайном: акцент
 * теперь белый и один на всё приложение, подменять в схеме нечего.
 */
@Composable
fun FilmaxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FilmaxDarkColorScheme,
        typography = FilmaxTypography,
        shapes = FilmaxShapes,
        content = content,
    )
}

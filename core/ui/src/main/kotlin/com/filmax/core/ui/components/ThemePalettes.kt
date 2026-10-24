package com.filmax.core.ui.components

import com.filmax.core.designsystem.FilmaxPalette
import com.filmax.core.designsystem.FilmaxPalettes
import com.filmax.core.domain.settings.ThemePalette

/**
 * Палитра дизайн-системы по выбору в Профиле. Одно соответствие на телефон: по нему тема красит
 * UI, а список выбора рисует кружки цветов.
 */
fun ThemePalette.toFilmaxPalette(): FilmaxPalette = when (this) {
    ThemePalette.VIOLET -> FilmaxPalettes.Violet
    ThemePalette.CORAL -> FilmaxPalettes.Coral
    ThemePalette.MINT -> FilmaxPalettes.Mint
}

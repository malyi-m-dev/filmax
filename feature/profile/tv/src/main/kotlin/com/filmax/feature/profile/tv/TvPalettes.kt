package com.filmax.feature.profile.tv

import com.filmax.core.domain.settings.ThemePalette
import com.filmax.core.tv.designsystem.Coral
import com.filmax.core.tv.designsystem.FxColors
import com.filmax.core.tv.designsystem.Mint
import com.filmax.core.tv.designsystem.Violet

/**
 * Палитра дизайн-системы для выбора в Профиле. Одно соответствие на приложение: по нему тема
 * красит ТВ-UI (FilmaxTvPaletteTheme в :app), а панель выбора рисует кружки цветов.
 */
fun ThemePalette.toFxColors(): FxColors = when (this) {
    ThemePalette.VIOLET -> Violet
    ThemePalette.CORAL -> Coral
    ThemePalette.MINT -> Mint
}

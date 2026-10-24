package com.filmax.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.domain.settings.ThemeSettingsRepository
import com.filmax.core.ui.components.toFilmaxPalette
import org.koin.compose.koinInject

/**
 * Тема телефона с палитрой, выбранной в Профиле. Выбор — поток настроек: переключили — весь UI
 * перекрашивается сразу, без перезапуска. Палитра общая с ТВ этого же устройства.
 */
@Composable
fun FilmaxPhonePaletteTheme(
    themeSettings: ThemeSettingsRepository = koinInject(),
    content: @Composable () -> Unit,
) {
    val palette by themeSettings.palette.collectAsState()
    FilmaxTheme(palette = palette.toFilmaxPalette(), content = content)
}

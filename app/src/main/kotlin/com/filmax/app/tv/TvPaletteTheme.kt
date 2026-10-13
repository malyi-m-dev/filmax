package com.filmax.app.tv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.filmax.core.domain.settings.ThemeSettingsRepository
import com.filmax.core.tv.designsystem.FilmaxTvTheme
import com.filmax.feature.profile.tv.toFxColors
import org.koin.compose.koinInject

/**
 * ТВ-тема с палитрой, выбранной в Профиле. Выбор — поток настроек: сменили палитру — весь ТВ-UI
 * перекрашивается сразу, без перезапуска и без возврата на экран.
 */
@Composable
fun FilmaxTvPaletteTheme(
    themeSettings: ThemeSettingsRepository = koinInject(),
    content: @Composable () -> Unit,
) {
    val palette by themeSettings.palette.collectAsState()
    FilmaxTvTheme(colors = palette.toFxColors(), content = content)
}

package com.filmax.data.watching

import com.filmax.core.domain.settings.ThemePalette
import com.filmax.core.domain.settings.ThemeSettingsRepository
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Палитра в настройках устройства — по имени, а не по номеру: порядок палитр может поменяться,
 * а сохранённый выбор должен остаться тем же. Неизвестное имя (палитру убрали) — палитра по
 * умолчанию.
 */
internal class ThemeSettingsRepositoryImpl(private val settings: Settings) : ThemeSettingsRepository {

    private val paletteState = MutableStateFlow(loadPalette())
    private val plainFocusState = MutableStateFlow(settings.getBoolean(KEY_PLAIN_FOCUS, false))

    override val palette: StateFlow<ThemePalette> = paletteState.asStateFlow()

    override val plainFocus: StateFlow<Boolean> = plainFocusState.asStateFlow()

    override suspend fun setPalette(palette: ThemePalette) {
        settings.putString(KEY_PALETTE, palette.name)
        paletteState.value = palette
    }

    override suspend fun setPlainFocus(enabled: Boolean) {
        settings.putBoolean(KEY_PLAIN_FOCUS, enabled)
        plainFocusState.value = enabled
    }

    private fun loadPalette(): ThemePalette =
        ThemePalette.entries.firstOrNull { it.name == settings.getStringOrNull(KEY_PALETTE) } ?: ThemePalette.VIOLET

    private companion object {
        const val KEY_PALETTE = "theme_palette"
        const val KEY_PLAIN_FOCUS = "theme_plain_focus"
    }
}

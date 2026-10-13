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

    private val state = MutableStateFlow(load())

    override val palette: StateFlow<ThemePalette> = state.asStateFlow()

    override suspend fun setPalette(palette: ThemePalette) {
        settings.putString(KEY_PALETTE, palette.name)
        state.value = palette
    }

    private fun load(): ThemePalette =
        ThemePalette.entries.firstOrNull { it.name == settings.getStringOrNull(KEY_PALETTE) } ?: ThemePalette.VIOLET

    private companion object {
        const val KEY_PALETTE = "theme_palette"
    }
}

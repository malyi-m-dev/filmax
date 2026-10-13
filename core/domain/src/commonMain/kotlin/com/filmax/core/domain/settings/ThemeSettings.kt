package com.filmax.core.domain.settings

import kotlinx.coroutines.flow.StateFlow

/**
 * Палитра ТВ-интерфейса, которую выбирают в Профиле. Сами цвета — в дизайн-системе ТВ, домен
 * хранит только выбор.
 */
enum class ThemePalette { VIOLET, CORAL, MINT }

/**
 * Настройки оформления на этом устройстве. [palette] — StateFlow, а не просто поток: тема
 * читает текущее значение сразу, и интерфейс не мигает палитрой по умолчанию при запуске.
 */
interface ThemeSettingsRepository {
    val palette: StateFlow<ThemePalette>

    suspend fun setPalette(palette: ThemePalette)
}

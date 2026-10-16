package com.filmax.core.domain.settings

import kotlinx.coroutines.flow.StateFlow

/**
 * Палитра ТВ-интерфейса, которую выбирают в Профиле. Сами цвета — в дизайн-системе ТВ, домен
 * хранит только выбор.
 */
enum class ThemePalette { VIOLET, CORAL, MINT }

/**
 * Настройки оформления на этом устройстве. Всё — StateFlow, а не просто потоки: тема читает
 * текущее значение сразу, и интерфейс не мигает значением по умолчанию при запуске.
 */
interface ThemeSettingsRepository {
    val palette: StateFlow<ThemePalette>

    /**
     * Фокус без белой обводки и свечения (ТВ, «Настройки UI» в Профиле): элемент в фокусе тогда
     * выделяют только увеличение и цвет подложки. По умолчанию выключено — обводка и свечение есть.
     */
    val plainFocus: StateFlow<Boolean>

    suspend fun setPalette(palette: ThemePalette)

    suspend fun setPlainFocus(enabled: Boolean)
}

package com.filmax.feature.profile.common

import com.filmax.core.domain.playback.PlaybackSettingsRepository
import com.filmax.core.domain.settings.ThemeSettingsRepository
import com.filmax.core.domain.source.ContentSourceRepository

/**
 * Настройки устройства, которые меняет Профиль: воспроизведение, оформление и источник
 * контента. Одним объектом — иначе конструктор [ProfileScreenModel] за порогом detekt.
 */
class ProfileSettings(
    val playback: PlaybackSettingsRepository,
    val theme: ThemeSettingsRepository,
    val sources: ContentSourceRepository,
)

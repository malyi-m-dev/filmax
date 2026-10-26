package com.filmax.feature.profile.common

import com.filmax.core.domain.user.model.DeviceSettings

/**
 * Состояние экрана «Настройки устройства».
 *
 * [settings] — рабочая копия: тумблеры и селекторы правят её локально, на сервер уходит только
 * по «Сохранить» ([DeviceSettingsEvent.Save]). Так экран не бьёт по сети на каждый клик и
 * повторяет поведение оригинального клиента kino.pub.
 */
data class DeviceSettingsState(
    val settings: DeviceSettings? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    /** Телефон: открыта шторка выбора типа потока. */
    val streamingSheetOpen: Boolean = false,
)

sealed interface DeviceSettingsEvent {
    /** Правка рабочей копии; до загрузки настроек события просто игнорируются. */
    sealed interface Edit : DeviceSettingsEvent

    data class SetSsl(val enabled: Boolean) : Edit
    data class SetHevc(val enabled: Boolean) : Edit
    data class SetHdr(val enabled: Boolean) : Edit
    data class Set4k(val enabled: Boolean) : Edit
    data class SetMixedPlaylist(val enabled: Boolean) : Edit
    data class SetStreamingType(val streamingType: Int) : Edit

    /** ТВ: тип потока по кругу — селектора с пульта нет, «ОК» на строке листает варианты. */
    data object CycleStreamingType : DeviceSettingsEvent

    data object OpenStreamingSheet : DeviceSettingsEvent
    data object CloseStreamingSheet : DeviceSettingsEvent

    /** На сервер — и после успеха назад в Профиль. */
    data object Save : DeviceSettingsEvent
    data object Back : DeviceSettingsEvent
}

/** Одноразовых эффектов нет: сохранённые настройки закрывают экран через навигатор в модели. */
sealed interface DeviceSettingsSideEffect

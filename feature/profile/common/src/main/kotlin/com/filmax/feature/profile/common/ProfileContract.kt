package com.filmax.feature.profile.common

import com.filmax.core.domain.playback.PlaybackSettings
import com.filmax.core.domain.settings.ThemePalette
import com.filmax.core.domain.user.model.UserProfile

data class ProfileState(
    val profile: UserProfile? = null,
    /** Кол-во просмотренного контента — из истории просмотров (`watching/history`). */
    val watchedCount: Int = 0,
    /** Кол-во элементов в избранном — из локального кэша favorites. */
    val favoritesCount: Int = 0,
    /** Максимальное качество устройства — из `device/info` (4K/HDR/HEVC/HD). */
    val quality: String? = null,
    /** Пользовательские настройки воспроизведения (качество/аудио/субтитры). */
    val playback: PlaybackSettings = PlaybackSettings(),
    /** Палитра ТВ-интерфейса. */
    val palette: ThemePalette = ThemePalette.VIOLET,
    /**
     * Настройка, чьи варианты показывает боковая панель (ТВ). Остаётся и после закрытия: панель
     * уезжает со своим содержимым, а фокус возвращается на строку этой настройки.
     */
    val setting: ProfileSetting = ProfileSetting.QUALITY,
    /** Панель вариантов открыта. */
    val settingOpen: Boolean = false,
    /** Открыт диалог «Выйти из аккаунта?» (ТВ). */
    val confirmLogout: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
)

/** Настройка со списком вариантов: её значение выбирают в боковой панели. */
enum class ProfileSetting(val label: String) {
    QUALITY("Качество видео"),
    AUDIO("Язык аудио"),
    SUBTITLES("Субтитры"),
    PALETTE("Палитра"),
}

sealed interface ProfileEvent {
    data object Logout : ProfileEvent
    data class SetQuality(val quality: String) : ProfileEvent
    data class SetAudioLanguage(val language: String) : ProfileEvent
    data class SetSubtitleLanguage(val language: String) : ProfileEvent

    /** «ОК» на строке со значением — панель с вариантами этой настройки. */
    data class OpenSetting(val setting: ProfileSetting) : ProfileEvent
    data object CloseSetting : ProfileEvent

    /** Вариант [index] из [optionsOf] открытой настройки: применить и закрыть панель. */
    data class ChooseOption(val index: Int) : ProfileEvent

    data object AskLogout : ProfileEvent
    data object DismissLogout : ProfileEvent
}

sealed interface ProfileSideEffect {
    /** Сессия завершена — экран должен увести пользователя на онбординг. */
    data object LoggedOut : ProfileSideEffect
}

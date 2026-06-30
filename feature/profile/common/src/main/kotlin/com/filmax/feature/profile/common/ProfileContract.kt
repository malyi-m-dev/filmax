package com.filmax.feature.profile.common

import com.filmax.core.domain.playback.PlaybackSettings
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
    val loading: Boolean = true,
    val error: String? = null,
)

sealed interface ProfileEvent {
    data object Logout : ProfileEvent
    data class SetQuality(val quality: String) : ProfileEvent
    data class SetAudioLanguage(val language: String) : ProfileEvent
    data class SetSubtitleLanguage(val language: String) : ProfileEvent
}

sealed interface ProfileSideEffect {
    /** Сессия завершена — экран должен увести пользователя на онбординг. */
    data object LoggedOut : ProfileSideEffect
}

package com.filmax.feature.profile

import com.filmax.core.domain.user.model.UserProfile

data class ProfileState(
    val profile: UserProfile? = null,
    /** Кол-во просмотренного контента — из истории просмотров (`watching/history`). */
    val watchedCount: Int = 0,
    /** Кол-во элементов в избранном — сумма по папкам закладок (`bookmarks`). */
    val favoritesCount: Int = 0,
    /** Максимальное качество устройства — из `device/info` (4K/HDR/HEVC/HD). */
    val quality: String? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

sealed interface ProfileEvent {
    data object Logout : ProfileEvent
}

sealed interface ProfileSideEffect {
    /** Сессия завершена — экран должен увести пользователя на онбординг. */
    data object LoggedOut : ProfileSideEffect
}

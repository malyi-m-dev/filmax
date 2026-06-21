package com.filmax.feature.profile

import com.filmax.core.domain.user.model.UserProfile

data class ProfileState(
    val profile: UserProfile? = null,
    val watchedCount: Int = 0,
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

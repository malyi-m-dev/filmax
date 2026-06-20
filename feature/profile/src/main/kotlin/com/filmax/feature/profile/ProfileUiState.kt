package com.filmax.feature.profile

import com.filmax.core.domain.user.model.UserProfile

data class ProfileUiState(
    val profile: UserProfile? = null,
    val watchedCount: Int = 0,
    val loading: Boolean = true,
    val error: String? = null,
)

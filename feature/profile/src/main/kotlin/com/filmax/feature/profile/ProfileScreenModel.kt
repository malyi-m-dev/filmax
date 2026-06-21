package com.filmax.feature.profile

import com.filmax.core.domain.auth.AuthRepository
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.presentation.BaseScreenModel

class ProfileScreenModel(
    private val user: UserRepository,
    private val auth: AuthRepository,
) : BaseScreenModel<ProfileState, ProfileSideEffect, ProfileEvent>(ProfileState()) {

    init {
        onFetchData()
    }

    override fun dispatch(event: ProfileEvent) {
        when (event) {
            ProfileEvent.Logout -> logout()
        }
    }

    override fun onFetchData() {
        screenModelScope {
            when (val result = user.getProfile()) {
                is RequestResult.Success ->
                    updateState { it.copy(loading = false, profile = result.data) }

                is RequestResult.Error ->
                    updateState { it.copy(loading = false, error = result.message) }
            }
        }
    }

    private fun logout() {
        screenModelScope {
            auth.logout()
            postSideEffect(ProfileSideEffect.LoggedOut)
        }
    }
}

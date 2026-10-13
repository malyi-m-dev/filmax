package com.filmax.app.navigation

import com.filmax.core.domain.auth.AuthRepository
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.user.model.initials
import com.filmax.core.presentation.BaseScreenModel
import com.filmax.feature.profile.common.label

class RootScreenModel(
    private val auth: AuthRepository,
    private val user: UserRepository,
) : BaseScreenModel<RootState, RootSideEffect, RootEvent>(RootState()) {

    init {
        onFetchData()
    }

    override fun dispatch(event: RootEvent) = Unit

    override fun onFetchData() {
        screenModelScope {
            auth.isAuthenticated.collect { value ->
                updateState { it.copy(isAuthenticated = value) }
                // Авторизованы — подтягиваем аккаунт для меню (best-effort).
                if (value == true) fetchAccount()
            }
        }
    }

    private suspend fun fetchAccount() {
        (user.getProfile() as? RequestResult.Success)?.let { result ->
            val profile = result.data
            updateState {
                it.copy(
                    account = AccountSummary(
                        name = profile.username,
                        subscription = profile.subscription.label(),
                        initials = profile.initials(),
                    ),
                )
            }
        }
    }
}

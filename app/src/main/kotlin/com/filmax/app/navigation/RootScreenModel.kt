package com.filmax.app.navigation

import com.filmax.core.domain.auth.AuthRepository
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.user.model.initials
import com.filmax.core.presentation.BaseScreenModel

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
                // Авторизованы — подтягиваем инициалы для аватара (best-effort).
                if (value == true) fetchUserInitials()
            }
        }
    }

    private suspend fun fetchUserInitials() {
        (user.getProfile() as? RequestResult.Success)?.let { result ->
            updateState { it.copy(initials = result.data.initials()) }
        }
    }
}

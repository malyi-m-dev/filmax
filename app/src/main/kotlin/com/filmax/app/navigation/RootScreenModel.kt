package com.filmax.app.navigation

import com.filmax.core.domain.auth.AuthRepository
import com.filmax.core.presentation.BaseScreenModel

class RootScreenModel(
    private val auth: AuthRepository,
) : BaseScreenModel<RootState, RootSideEffect, RootEvent>(RootState()) {

    init {
        onFetchData()
    }

    override fun dispatch(event: RootEvent) = Unit

    override fun onFetchData() {
        screenModelScope {
            auth.isAuthenticated.collect { value ->
                updateState { it.copy(isAuthenticated = value) }
            }
        }
    }
}

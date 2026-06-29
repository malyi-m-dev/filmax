package com.filmax.tv.navigation

import com.filmax.core.domain.auth.AuthRepository
import com.filmax.core.presentation.BaseScreenModel

class TvRootScreenModel(
    private val auth: AuthRepository,
) : BaseScreenModel<TvRootState, TvRootSideEffect, TvRootEvent>(TvRootState()) {

    init {
        onFetchData()
    }

    override fun dispatch(event: TvRootEvent) = Unit

    override fun onFetchData() {
        screenModelScope {
            auth.isAuthenticated.collect { value ->
                updateState { it.copy(isAuthenticated = value) }
            }
        }
    }
}

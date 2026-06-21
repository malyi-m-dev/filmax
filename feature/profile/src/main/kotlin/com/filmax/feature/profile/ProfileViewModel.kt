package com.filmax.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmax.core.domain.auth.AuthRepository
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.user.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val user: UserRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            when (val result = user.getProfile()) {
                is RequestResult.Success ->
                    _state.update { it.copy(loading = false, profile = result.data) }

                is RequestResult.Error ->
                    _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            auth.logout()
        }
    }
}

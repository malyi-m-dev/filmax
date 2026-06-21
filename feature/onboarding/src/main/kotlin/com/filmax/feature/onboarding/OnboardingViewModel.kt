package com.filmax.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmax.core.domain.auth.AuthRepository
import com.filmax.core.domain.common.RequestResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state = _state.asStateFlow()

    fun nextStep() {
        val next = _state.value.step + 1
        _state.update { it.copy(step = next) }
        if (next == 2) requestDeviceCode()
    }

    fun prevStep() {
        _state.update { it.copy(step = maxOf(0, it.step - 1), error = null) }
    }

    private fun requestDeviceCode() {
        viewModelScope.launch {
            when (val result = auth.requestDeviceCode()) {
                is RequestResult.Success -> {
                    val dc = result.data
                    _state.update {
                        it.copy(
                            deviceCode = dc.code,
                            userCode = dc.userCode,
                            verificationUri = dc.verificationUri,
                            polling = true,
                        )
                    }
                    pollForToken(dc.code, dc.interval, dc.expiresIn)
                }

                is RequestResult.Error -> _state.update { it.copy(error = result.message) }
            }
        }
    }

    private suspend fun pollForToken(code: String, intervalSec: Int, expiresIn: Int) {
        val startMs = System.currentTimeMillis()
        val timeoutMs = expiresIn * 1000L
        while (System.currentTimeMillis() - startMs < timeoutMs) {
            delay(intervalSec * 1000L)
            // Success — авторизовались; Error — ещё не подтверждено, продолжаем поллинг.
            val result = auth.pollForToken(
                code = code,
                username = "",
                timestamp = System.currentTimeMillis() / 1000L,
            )
            if (result is RequestResult.Success) {
                _state.update { it.copy(polling = false, authenticated = true) }
                return
            }
        }
        _state.update { it.copy(polling = false, error = "Время ожидания истекло. Попробуйте снова.") }
    }

    fun retryDeviceCode() {
        _state.update { it.copy(error = null, deviceCode = null, userCode = null) }
        requestDeviceCode()
    }
}

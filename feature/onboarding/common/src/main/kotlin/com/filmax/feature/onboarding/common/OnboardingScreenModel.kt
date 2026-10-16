package com.filmax.feature.onboarding.common

import com.filmax.core.domain.auth.AuthRepository
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.presentation.BaseScreenModel
import kotlinx.coroutines.delay

private const val MILLIS_PER_SECOND = 1000L

class OnboardingScreenModel(
    private val auth: AuthRepository,
) : BaseScreenModel<OnboardingState, OnboardingSideEffect, OnboardingEvent>(OnboardingState()) {

    override fun onEvent(event: OnboardingEvent) {
        when (event) {
            OnboardingEvent.NextStep -> nextStep()
            OnboardingEvent.PrevStep -> screenModelScope { snapshot ->
                updateState { it.copy(step = maxOf(0, it.step - 1), error = null) }
            }
            OnboardingEvent.RetryDeviceCode -> retryDeviceCode()
        }
    }

    /** Данные не грузятся при старте — код запрашивается при переходе на шаг авторизации. */
    override fun onFetchData() = Unit

    private fun nextStep() {
        screenModelScope { snapshot ->
            val next = state.step + 1
            updateState { it.copy(step = next) }
            if (next == 2) requestDeviceCode()
        }
    }

    private fun requestDeviceCode() {
        screenModelScope { snapshot ->
            when (val result = auth.requestDeviceCode()) {
                is RequestResult.Success -> {
                    val deviceAuth = result.data
                    updateState {
                        it.copy(
                            deviceCode = deviceAuth.code,
                            userCode = deviceAuth.userCode,
                            verificationUri = deviceAuth.verificationUri,
                            polling = true,
                        )
                    }
                    pollForToken(deviceAuth.code, deviceAuth.interval, deviceAuth.expiresIn)
                }

                is RequestResult.Error -> updateState { it.copy(error = result.message) }
            }
        }
    }

    private suspend fun pollForToken(code: String, intervalSec: Int, expiresIn: Int) {
        val startMs = System.currentTimeMillis()
        val timeoutMs = expiresIn * MILLIS_PER_SECOND
        while (System.currentTimeMillis() - startMs < timeoutMs) {
            delay(intervalSec * MILLIS_PER_SECOND)
            // Success — авторизовались; Error — ещё не подтверждено, продолжаем поллинг.
            val result = auth.pollForToken(
                code = code,
                username = "",
                timestamp = System.currentTimeMillis() / MILLIS_PER_SECOND,
            )
            if (result is RequestResult.Success) {
                // Подпись устройства в аккаунте — не здесь, а в RootScreenModel: этот экран
                // снимается навигацией, как только сохранится токен, и запрос не успевал уйти.
                updateState { it.copy(polling = false) }
                postSideEffect(OnboardingSideEffect.Authenticated)
                return
            }
        }
        updateState { it.copy(polling = false, error = "Время ожидания истекло. Попробуйте снова.") }
    }

    private fun retryDeviceCode() {
        screenModelScope { snapshot ->
            updateState { it.copy(error = null, deviceCode = null, userCode = null) }
            requestDeviceCode()
        }
    }
}

package com.filmax.feature.onboarding.common

import com.filmax.core.domain.auth.AuthRepository
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.presentation.BaseScreenModel
import kotlinx.coroutines.delay

private const val MILLIS_PER_SECOND = 1000L

/**
 * Вход по device-flow: код активации, опрос kino.pub до подтверждения и переход на Главную.
 * Общий для телефона и ТВ — экраны только рисуют шаг и код.
 */
class OnboardingScreenModel(
    private val auth: AuthRepository,
    private val now: () -> Long = System::currentTimeMillis,
) : BaseScreenModel<OnboardingState, OnboardingSideEffect, OnboardingEvent>(OnboardingState()) {

    override fun onEvent(event: OnboardingEvent) {
        when (event) {
            OnboardingEvent.Login -> onLogin()
            OnboardingEvent.RetryDeviceCode -> onRetryDeviceCode()
        }
    }

    /** Данные не грузятся при старте — код запрашивается при переходе на шаг авторизации. */
    override fun onFetchData() = Unit

    private fun onLogin() {
        screenModelScope { _ ->
            updateState { it.copy(step = OnboardingStep.AUTH) }
            requestDeviceCode()
        }
    }

    private fun onRetryDeviceCode() {
        screenModelScope { _ ->
            updateState { it.copy(error = null, deviceCode = null, userCode = null) }
            requestDeviceCode()
        }
    }

    private suspend fun requestDeviceCode() {
        when (val result = auth.requestDeviceCode()) {
            is RequestResult.Success -> {
                val deviceAuth = result.data
                updateState {
                    it.copy(
                        deviceCode = deviceAuth.code,
                        userCode = deviceAuth.userCode,
                        verificationUri = deviceAuth.verificationUri.ifBlank { DEFAULT_VERIFICATION_URI },
                        polling = true,
                    )
                }
                pollForToken(deviceAuth.code, deviceAuth.interval, deviceAuth.expiresIn)
            }

            is RequestResult.Error -> updateState { it.copy(error = result.message) }
        }
    }

    private suspend fun pollForToken(code: String, intervalSec: Int, expiresIn: Int) {
        val startMs = now()
        val timeoutMs = expiresIn * MILLIS_PER_SECOND
        while (now() - startMs < timeoutMs) {
            delay(intervalSec * MILLIS_PER_SECOND)
            // Success — авторизовались; Error — ещё не подтверждено, продолжаем поллинг.
            val result = auth.pollForToken(code = code, username = "", timestamp = now() / MILLIS_PER_SECOND)
            if (result is RequestResult.Success) {
                // Подпись устройства в аккаунте — не здесь, а в RootScreenModel: этот экран
                // снимается навигацией, как только сохранится токен, и запрос не успевал уйти.
                updateState { it.copy(polling = false) }
                postSideEffect(OnboardingSideEffect.SignedIn)
                return
            }
        }
        updateState { it.copy(polling = false, error = "Время ожидания истекло. Попробуйте снова.") }
    }
}

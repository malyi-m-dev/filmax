package com.filmax.feature.onboarding.common

import com.filmax.core.domain.auth.AuthRepository
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.source.ContentSource
import com.filmax.core.domain.source.ContentSourceRepository
import com.filmax.core.presentation.BaseScreenModel
import kotlinx.coroutines.delay

private const val MILLIS_PER_SECOND = 1000L

/**
 * Вход в kino.pub по коду устройства — и развилка на вход в домашний сервер. Если источник уже
 * домашний сервер (выбрали в Профиле или вышли из него), экран сразу открывается на сервере.
 */
class OnboardingScreenModel(
    private val auth: AuthRepository,
    private val sources: ContentSourceRepository,
) : BaseScreenModel<OnboardingState, OnboardingSideEffect, OnboardingEvent>(
    OnboardingState(homeServer = sources.source.value == ContentSource.JELLYFIN),
) {

    override fun onEvent(event: OnboardingEvent) {
        when (event) {
            OnboardingEvent.Login -> login()
            OnboardingEvent.RetryDeviceCode -> retryDeviceCode()
            OnboardingEvent.OpenHomeServer -> screenModelScope { updateState { it.copy(homeServer = true) } }
            OnboardingEvent.CloseHomeServer -> screenModelScope {
                sources.select(ContentSource.KINOPUB)
                updateState { it.copy(homeServer = false) }
            }
        }
    }

    /** Данные не грузятся при старте — код запрашивается при переходе на шаг авторизации. */
    override fun onFetchData() = Unit

    /**
     * Шаг «фичи» между приветствием и кодом убран из макета: раньше модель его хранила, а экраны
     * проматывали сами. Теперь «Войти» ведёт сразу на код, и повторное нажатие кода не перезапросит.
     */
    private fun login() {
        if (state.step == OnboardingStep.AUTH) return
        screenModelScope {
            updateState { it.copy(step = OnboardingStep.AUTH, error = null) }
            requestDeviceCode()
        }
    }

    private fun requestDeviceCode() {
        screenModelScope {
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
        screenModelScope {
            updateState { it.copy(error = null, deviceCode = null, userCode = null) }
            requestDeviceCode()
        }
    }
}

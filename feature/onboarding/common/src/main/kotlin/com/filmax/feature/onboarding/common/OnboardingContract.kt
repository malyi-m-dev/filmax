package com.filmax.feature.onboarding.common

/** Два шага входа: приветствие с «Войти» и активация устройства по коду. */
enum class OnboardingStep { WELCOME, AUTH }

/**
 * Состояние входа. [verificationUri] — адрес, который набирают в браузере; пока kino.pub не
 * ответил, показываем известный заранее: подсказка не должна ждать код.
 */
data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val deviceCode: String? = null,
    val userCode: String? = null,
    val verificationUri: String = DEFAULT_VERIFICATION_URI,
    val polling: Boolean = false,
    val error: String? = null,
)

const val DEFAULT_VERIFICATION_URI = "kinopub.me/device"

sealed interface OnboardingEvent {
    /** «Войти» — на шаг активации; код запрашивается тут же. */
    data object Login : OnboardingEvent
    data object RetryDeviceCode : OnboardingEvent
}

/** Одноразовых эффектов нет: вход делает Главную корнем через навигатор в модели. */
sealed interface OnboardingSideEffect

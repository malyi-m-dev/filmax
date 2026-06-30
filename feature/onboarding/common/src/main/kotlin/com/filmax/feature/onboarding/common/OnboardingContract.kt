package com.filmax.feature.onboarding.common

data class OnboardingState(
    val step: Int = 0,
    val deviceCode: String? = null,
    val userCode: String? = null,
    val verificationUri: String? = null,
    val polling: Boolean = false,
    val error: String? = null,
)

sealed interface OnboardingEvent {
    data object NextStep : OnboardingEvent
    data object PrevStep : OnboardingEvent
    data object RetryDeviceCode : OnboardingEvent
}

sealed interface OnboardingSideEffect {
    /** Устройство авторизовано — экран должен увести пользователя дальше. */
    data object Authenticated : OnboardingSideEffect
}

package com.filmax.feature.onboarding.common

data class OnboardingState(
    val step: Int = 0,
    val deviceCode: String? = null,
    val userCode: String? = null,
    val verificationUri: String? = null,
    val polling: Boolean = false,
    val error: String? = null,
    /** Показан вход на домашний сервер, а не kino.pub. */
    val homeServer: Boolean = false,
)

sealed interface OnboardingEvent {
    data object NextStep : OnboardingEvent
    data object PrevStep : OnboardingEvent
    data object RetryDeviceCode : OnboardingEvent

    /** «Домашний сервер» на приветствии. */
    data object OpenHomeServer : OnboardingEvent

    /** Назад к kino.pub: источник снова kino.pub, и если вход в него есть — граф уведёт на Главную. */
    data object CloseHomeServer : OnboardingEvent
}

sealed interface OnboardingSideEffect {
    /** Устройство авторизовано — экран должен увести пользователя дальше. */
    data object Authenticated : OnboardingSideEffect
}

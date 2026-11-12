// Неизменные образцы входа — для превью телефона и ТВ: приветствие, код устройства, ошибка.
package com.filmax.feature.onboarding.common

object OnboardingSamples {

    val welcome = OnboardingState()

    /** Код получен, ждём подтверждения на сайте. */
    val code = OnboardingState(
        step = OnboardingStep.AUTH,
        deviceCode = "device",
        userCode = "7KQ2-M4XP",
        verificationUri = "kino.pub/device",
        polling = true,
    )

    /** Код не получили — экран предлагает повторить. */
    val failed = OnboardingState(step = OnboardingStep.AUTH, error = "Нет сети")

    val all: List<OnboardingState> = listOf(welcome, code, failed)
}

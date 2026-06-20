package com.filmax.feature.onboarding

data class OnboardingUiState(
    val step: Int = 0,
    val deviceCode: String? = null,
    val userCode: String? = null,
    val verificationUri: String? = null,
    val polling: Boolean = false,
    val error: String? = null,
    val authenticated: Boolean = false,
)

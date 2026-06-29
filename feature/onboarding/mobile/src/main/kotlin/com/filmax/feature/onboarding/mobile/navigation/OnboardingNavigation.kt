package com.filmax.feature.onboarding.mobile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.onboarding.mobile.OnboardingScreen
import kotlinx.serialization.Serializable

@Serializable
object OnboardingRoute

fun NavGraphBuilder.onboardingScreen(onAuthenticated: () -> Unit) {
    composable<OnboardingRoute> {
        OnboardingScreen(onAuthenticated = onAuthenticated)
    }
}

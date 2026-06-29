package com.filmax.feature.tv.onboarding.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.tv.onboarding.TvOnboardingScreen
import kotlinx.serialization.Serializable

@Serializable
object TvOnboardingRoute

fun NavGraphBuilder.tvOnboardingScreen(onAuthenticated: () -> Unit) {
    composable<TvOnboardingRoute> {
        TvOnboardingScreen(onAuthenticated = onAuthenticated)
    }
}

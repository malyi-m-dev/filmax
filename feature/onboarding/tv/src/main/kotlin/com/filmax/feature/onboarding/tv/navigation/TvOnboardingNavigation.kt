package com.filmax.feature.onboarding.tv.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.onboarding.tv.TvOnboardingScreen
import kotlinx.serialization.Serializable

@Serializable
object TvOnboardingRoute

fun NavGraphBuilder.tvOnboardingScreen() {
    composable<TvOnboardingRoute> {
        TvOnboardingScreen()
    }
}

package com.filmax.tv.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.filmax.feature.tv.home.navigation.TvHomeRoute
import com.filmax.feature.tv.home.navigation.tvHomeScreen
import com.filmax.feature.tv.onboarding.navigation.TvOnboardingRoute
import com.filmax.feature.tv.onboarding.navigation.tvOnboardingScreen
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
private object TvSplashRoute

@Composable
fun FilmaxTvNavGraph(
    rootScreenModel: TvRootScreenModel = koinViewModel(),
) {
    val rootState by rootScreenModel.collectAsState()
    val isAuthenticated = rootState.isAuthenticated
    val navController = rememberNavController()

    LaunchedEffect(isAuthenticated) {
        val auth = isAuthenticated ?: return@LaunchedEffect
        if (auth) {
            navController.navigate(TvHomeRoute) {
                popUpTo(TvSplashRoute) { inclusive = true }
            }
        } else {
            navController.navigate(TvOnboardingRoute) {
                popUpTo(TvSplashRoute) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        NavHost(
            navController = navController,
            startDestination = TvSplashRoute,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() },
        ) {
            composable<TvSplashRoute> {
                Box(Modifier.fillMaxSize())
            }

            tvOnboardingScreen(
                onAuthenticated = {
                    navController.navigate(TvHomeRoute) {
                        popUpTo(TvOnboardingRoute) { inclusive = true }
                    }
                },
            )

            tvHomeScreen(
                // Экраны деталей/плеера для TV ещё не реализованы — открытие айтема пока no-op.
                onOpenItem = { },
            )
        }
    }
}

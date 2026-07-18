package com.filmax.feature.player.mobile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.filmax.feature.player.common.navigation.TrailerRoute
import com.filmax.feature.player.mobile.TrailerScreen

fun NavGraphBuilder.trailerScreen(onBack: () -> Unit) {
    composable<TrailerRoute> { entry ->
        val route = entry.toRoute<TrailerRoute>()
        TrailerScreen(url = route.url, title = route.title, onBack = onBack)
    }
}

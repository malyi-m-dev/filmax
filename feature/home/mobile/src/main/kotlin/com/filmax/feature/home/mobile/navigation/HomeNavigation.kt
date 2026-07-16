package com.filmax.feature.home.mobile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.home.mobile.HomeActions
import com.filmax.feature.home.mobile.HomeScreen
import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

fun NavGraphBuilder.homeScreen(actions: HomeActions) {
    composable<HomeRoute> {
        HomeScreen(actions = actions)
    }
}

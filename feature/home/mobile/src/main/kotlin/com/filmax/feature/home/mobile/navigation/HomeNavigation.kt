package com.filmax.feature.home.mobile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.home.mobile.HomeScreen
import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

fun NavGraphBuilder.homeScreen(onOpenItem: (Int) -> Unit) {
    composable<HomeRoute> {
        HomeScreen(onOpenItem = onOpenItem)
    }
}

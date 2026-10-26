package com.filmax.feature.player.mobile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.player.common.navigation.TrailerRoute
import com.filmax.feature.player.mobile.TrailerScreen

fun NavGraphBuilder.trailerScreen() {
    composable<TrailerRoute> { TrailerScreen() }
}

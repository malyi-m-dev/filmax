package com.filmax.feature.player.mobile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.player.mobile.PlayerScreen
import com.filmax.feature.player.navigation.PlayerRoute

fun NavGraphBuilder.playerScreen(onBack: () -> Unit) {
    composable<PlayerRoute> {
        PlayerScreen(onBack = onBack)
    }
}

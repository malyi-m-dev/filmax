package com.filmax.feature.player.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.player.PlayerScreen
import kotlinx.serialization.Serializable

@Serializable
data class PlayerRoute(val itemId: Int)

fun NavGraphBuilder.playerScreen(onBack: () -> Unit) {
    composable<PlayerRoute> {
        PlayerScreen(onBack = onBack)
    }
}

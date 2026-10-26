package com.filmax.feature.player.mobile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.player.common.navigation.PlayerRoute
import com.filmax.feature.player.mobile.PlayerScreen

/** Мобильный плеер на том же маршруте [PlayerRoute], что и TV-фича; маршрут читает модель. */
fun NavGraphBuilder.playerScreen() {
    composable<PlayerRoute> { PlayerScreen() }
}

package com.filmax.feature.player.tv.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.player.common.navigation.TrailerRoute
import com.filmax.feature.player.tv.TvTrailerScreen

/** Регистрирует TV-экран трейлера на ТОТ ЖЕ маршрут [TrailerRoute], что и мобильная фича. */
fun NavGraphBuilder.tvTrailerScreen() {
    composable<TrailerRoute> { TvTrailerScreen() }
}

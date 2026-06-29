package com.filmax.feature.tv.player.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.player.navigation.PlayerRoute
import com.filmax.feature.tv.player.TvPlayerScreen

/**
 * Регистрирует TV-плеер на ТОТ ЖЕ маршрут [PlayerRoute], что и мобильная фича —
 * [com.filmax.feature.player.PlayerScreenModel] получает itemId из SavedStateHandle.
 */
fun NavGraphBuilder.tvPlayerScreen(onBack: () -> Unit) {
    composable<PlayerRoute> {
        TvPlayerScreen(onBack = onBack)
    }
}

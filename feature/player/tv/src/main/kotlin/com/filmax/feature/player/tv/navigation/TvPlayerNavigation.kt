package com.filmax.feature.player.tv.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.player.common.navigation.PlayerRoute
import com.filmax.feature.player.tv.TvPlayerScreen

/**
 * Регистрирует TV-плеер на ТОТ ЖЕ маршрут [PlayerRoute], что и мобильная фича —
 * [com.filmax.feature.player.common.PlayerScreenModel] получает itemId из SavedStateHandle.
 */
fun NavGraphBuilder.tvPlayerScreen(onBack: () -> Unit) {
    composable<PlayerRoute> {
        TvPlayerScreen(onBack = onBack)
    }
}

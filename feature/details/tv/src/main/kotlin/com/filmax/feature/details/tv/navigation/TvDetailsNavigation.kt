package com.filmax.feature.details.tv.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.details.navigation.DetailsRoute
import com.filmax.feature.details.tv.TvDetailsScreen

/**
 * Регистрирует TV-экран деталей на ТОТ ЖЕ маршрут [DetailsRoute], что и мобильная фича —
 * благодаря этому [com.filmax.feature.details.DetailsScreenModel] получает itemId из SavedStateHandle.
 */
fun NavGraphBuilder.tvDetailsScreen(
    onPlay: (Int) -> Unit,
    onOpenItem: (Int) -> Unit,
) {
    composable<DetailsRoute> {
        TvDetailsScreen(onPlay = onPlay, onOpenItem = onOpenItem)
    }
}

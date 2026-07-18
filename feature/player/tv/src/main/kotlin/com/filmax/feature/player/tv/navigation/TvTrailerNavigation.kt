package com.filmax.feature.player.tv.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.filmax.feature.player.common.navigation.TrailerRoute
import com.filmax.feature.player.tv.TvTrailerScreen

/**
 * Регистрирует TV-экран трейлера на ТОТ ЖЕ маршрут [TrailerRoute], что и мобильная фича.
 *
 * Отдельного ScreenModel нет: [TrailerRoute.url] — готовый временный HLS-плейлист (.m3u8) с
 * истекающим токеном, поэтому играем его как есть штатным контроллером Media3, а «Назад» пульта
 * закрывает экран ([onBack]).
 */
fun NavGraphBuilder.tvTrailerScreen(onBack: () -> Unit) {
    composable<TrailerRoute> { entry ->
        val route = entry.toRoute<TrailerRoute>()
        TvTrailerScreen(url = route.url, title = route.title, onBack = onBack)
    }
}

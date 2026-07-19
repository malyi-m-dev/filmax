package com.filmax.feature.details.tv.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.details.common.navigation.DetailsRoute
import com.filmax.feature.details.tv.TvDetailsNav
import com.filmax.feature.details.tv.TvDetailsScreen

/**
 * Регистрирует TV-экран деталей на ТОТ ЖЕ маршрут [DetailsRoute], что и мобильная фича —
 * благодаря этому [com.filmax.feature.details.common.DetailsScreenModel] получает itemId из SavedStateHandle.
 *
 * [onOpenPerson] ведёт в фильмографию актёра/режиссёра, [onPlayTrailer] — в TV-плеер трейлера.
 */
fun NavGraphBuilder.tvDetailsScreen(
    onPlay: (itemId: Int, season: Int, videoId: Int) -> Unit,
    onOpenItem: (Int) -> Unit,
    onOpenPerson: (name: String, isDirector: Boolean) -> Unit,
    onPlayTrailer: (url: String, title: String) -> Unit,
) {
    composable<DetailsRoute> {
        TvDetailsScreen(
            nav = TvDetailsNav(
                onPlay = onPlay,
                onOpenItem = onOpenItem,
                onOpenPerson = onOpenPerson,
                onPlayTrailer = onPlayTrailer,
            ),
        )
    }
}

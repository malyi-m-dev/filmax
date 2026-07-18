package com.filmax.feature.details.mobile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.details.common.navigation.DetailsRoute
import com.filmax.feature.details.mobile.DetailsNav
import com.filmax.feature.details.mobile.DetailsScreen

/**
 * [onPlay] принимает НОМЕР серии вторым аргументом (как на TV): kino.pub и принимает, и отдаёт
 * прогресс по номеру видео, а не по id трека. Фильм играется целиком — экран передаёт `-1`,
 * что совпадает с дефолтом `PlayerRoute.videoId`.
 *
 * [onOpenPerson] ведёт в фильмографию актёра/режиссёра, [onPlayTrailer] — в плеер трейлера
 * (прямой HLS-url из `item.trailer`).
 */
fun NavGraphBuilder.detailsScreen(
    onBack: () -> Unit,
    onPlay: (itemId: Int, videoId: Int) -> Unit,
    onOpenItem: (Int) -> Unit,
    onOpenPerson: (name: String, isDirector: Boolean) -> Unit,
    onPlayTrailer: (url: String, title: String) -> Unit,
) {
    composable<DetailsRoute> {
        DetailsScreen(
            nav = DetailsNav(
                onBack = onBack,
                onPlay = onPlay,
                onOpenItem = onOpenItem,
                onOpenPerson = onOpenPerson,
                onPlayTrailer = onPlayTrailer,
            ),
        )
    }
}

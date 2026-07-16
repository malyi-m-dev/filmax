package com.filmax.feature.details.mobile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.details.common.navigation.DetailsRoute
import com.filmax.feature.details.mobile.DetailsScreen

/**
 * [onPlay] принимает НОМЕР серии вторым аргументом (как на TV): kino.pub и принимает, и отдаёт
 * прогресс по номеру видео, а не по id трека. Фильм играется целиком — экран передаёт `-1`,
 * что совпадает с дефолтом `PlayerRoute.videoId`.
 */
fun NavGraphBuilder.detailsScreen(
    onBack: () -> Unit,
    onPlay: (itemId: Int, videoId: Int) -> Unit,
    onOpenItem: (Int) -> Unit,
) {
    composable<DetailsRoute> {
        DetailsScreen(onBack = onBack, onPlay = onPlay, onOpenItem = onOpenItem)
    }
}

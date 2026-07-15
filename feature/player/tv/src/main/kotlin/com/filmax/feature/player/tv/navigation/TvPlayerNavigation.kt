package com.filmax.feature.player.tv.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.filmax.feature.player.common.navigation.PlayerRoute
import com.filmax.feature.player.tv.TvPlayerScreen

/**
 * Регистрирует TV-плеер на ТОТ ЖЕ маршрут [PlayerRoute], что и мобильная фича —
 * [com.filmax.feature.player.common.PlayerScreenModel] получает itemId из SavedStateHandle.
 *
 * [onPlayEpisode] — переход на следующую серию. Это именно навигация на новый экран плеера:
 * прогресс просмотра модель пишет в трек, выбранный при старте, поэтому подмена MediaItem на месте
 * писала бы позицию новой серии в запись предыдущей. Пока колбэк не задан, пункт «Следующая серия»
 * в настройках не показывается.
 */
fun NavGraphBuilder.tvPlayerScreen(
    onBack: () -> Unit,
    onPlayEpisode: ((itemId: Int, videoId: Int) -> Unit)? = null,
) {
    composable<PlayerRoute> { entry ->
        val route = entry.toRoute<PlayerRoute>()
        TvPlayerScreen(
            onBack = onBack,
            videoId = route.videoId,
            onPlayEpisode = onPlayEpisode?.let { play -> { videoId -> play(route.itemId, videoId) } },
        )
    }
}

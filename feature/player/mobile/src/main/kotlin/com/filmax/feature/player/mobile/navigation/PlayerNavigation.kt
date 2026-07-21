package com.filmax.feature.player.mobile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.filmax.feature.player.common.navigation.PlayerRoute
import com.filmax.feature.player.mobile.PlayerNav
import com.filmax.feature.player.mobile.PlayerScreen

/**
 * Мобильный плеер на том же маршруте [PlayerRoute], что и TV-фича.
 *
 * [onPlayEpisode] — переход на следующую серию. Как и на TV, это навигация на НОВЫЙ экран
 * плеера: прогресс просмотра модель пишет в трек, выбранный при старте, и подмена MediaItem
 * на месте писала бы позицию новой серии в запись предыдущей. Пока колбэк не задан, кнопка
 * «Следующая серия» и автопереход не показываются.
 */
fun NavGraphBuilder.playerScreen(
    onBack: () -> Unit,
    onPlayEpisode: ((itemId: Int, season: Int, videoId: Int) -> Unit)? = null,
) {
    composable<PlayerRoute> { entry ->
        val route = entry.toRoute<PlayerRoute>()

        // Локальная функция вместо лямбды-в-лямбде: экрану нужен колбэк без itemId (он у маршрута).
        fun playEpisode(season: Int, videoId: Int) {
            onPlayEpisode?.invoke(route.itemId, season, videoId)
        }
        PlayerScreen(
            onBack = onBack,
            nav = PlayerNav(
                videoId = route.videoId,
                season = route.season,
                onPlayEpisode = onPlayEpisode?.let { ::playEpisode },
            ),
        )
    }
}

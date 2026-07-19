package com.filmax.feature.player.tv.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.filmax.feature.player.common.navigation.PlayerRoute
import com.filmax.feature.player.tv.TvPlayerNav
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
    onPlayEpisode: ((itemId: Int, season: Int, videoId: Int) -> Unit)? = null,
) {
    composable<PlayerRoute> { entry ->
        val route = entry.toRoute<PlayerRoute>()

        // Локальная функция вместо лямбды-в-лямбде: экрану нужен колбэк без itemId (он у маршрута).
        fun playEpisode(season: Int, videoId: Int) {
            onPlayEpisode?.invoke(route.itemId, season, videoId)
        }
        TvPlayerScreen(
            onBack = onBack,
            nav = TvPlayerNav(
                videoId = route.videoId,
                season = route.season,
                onPlayEpisode = onPlayEpisode?.let { ::playEpisode },
            ),
        )
    }
}

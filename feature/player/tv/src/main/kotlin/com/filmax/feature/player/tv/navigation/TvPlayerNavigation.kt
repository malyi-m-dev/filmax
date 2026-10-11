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
 * `itemId` экран получает отсюда, а не из состояния: тайтл нужен ему, чтобы попросить следующую
 * серию. Это именно навигация на новый экран плеера (замена текущего): прогресс просмотра модель
 * пишет в трек, выбранный при старте, поэтому подмена MediaItem на месте писала бы позицию новой
 * серии в запись предыдущей.
 */
fun NavGraphBuilder.tvPlayerScreen() {
    composable<PlayerRoute> { entry ->
        val route = entry.toRoute<PlayerRoute>()
        TvPlayerScreen(itemId = route.itemId)
    }
}

package com.filmax.feature.search.tv.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.search.common.navigation.FilmographyRoute
import com.filmax.feature.search.tv.TvFilmographyScreen

/**
 * Регистрирует TV-экран «Фильмография» на ТОТ ЖЕ маршрут [FilmographyRoute], что и мобильная
 * фича — благодаря этому [com.filmax.feature.search.common.FilmographyScreenModel] получает имя и
 * признак режиссёра из SavedStateHandle.
 */
fun NavGraphBuilder.tvFilmographyScreen(onBack: () -> Unit, onOpenItem: (Int) -> Unit) {
    composable<FilmographyRoute> {
        TvFilmographyScreen(onBack = onBack, onOpenItem = onOpenItem)
    }
}

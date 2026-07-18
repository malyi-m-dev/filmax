package com.filmax.feature.search.mobile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.search.common.navigation.FilmographyRoute
import com.filmax.feature.search.mobile.FilmographyScreen

/** Регистрация экрана «Фильмография» в графе навигации. */
fun NavGraphBuilder.filmographyScreen(onBack: () -> Unit, onOpenItem: (Int) -> Unit) {
    composable<FilmographyRoute> {
        FilmographyScreen(onBack = onBack, onOpenItem = onOpenItem)
    }
}

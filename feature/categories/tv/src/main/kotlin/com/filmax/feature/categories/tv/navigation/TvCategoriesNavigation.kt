package com.filmax.feature.categories.tv.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.categories.tv.TvCategoriesScreen
import kotlinx.serialization.Serializable

@Serializable
object TvCategoriesRoute

fun NavGraphBuilder.tvCategoriesScreen(onOpenGenre: (String) -> Unit) {
    composable<TvCategoriesRoute> {
        TvCategoriesScreen(onOpenGenre = onOpenGenre)
    }
}

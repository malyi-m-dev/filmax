package com.filmax.feature.tv.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.tv.home.TvHomeScreen
import kotlinx.serialization.Serializable

@Serializable
object TvHomeRoute

fun NavGraphBuilder.tvHomeScreen(onOpenItem: (Int) -> Unit) {
    composable<TvHomeRoute> {
        TvHomeScreen(onOpenItem = onOpenItem)
    }
}

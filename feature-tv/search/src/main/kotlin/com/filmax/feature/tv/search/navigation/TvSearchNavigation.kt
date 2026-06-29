package com.filmax.feature.tv.search.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.tv.search.TvSearchScreen
import kotlinx.serialization.Serializable

@Serializable
object TvSearchRoute

fun NavGraphBuilder.tvSearchScreen(onOpenItem: (Int) -> Unit) {
    composable<TvSearchRoute> {
        TvSearchScreen(onOpenItem = onOpenItem)
    }
}

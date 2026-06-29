package com.filmax.feature.search.tv.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.search.tv.TvSearchScreen
import kotlinx.serialization.Serializable

@Serializable
object TvSearchRoute

fun NavGraphBuilder.tvSearchScreen(onOpenItem: (Int) -> Unit) {
    composable<TvSearchRoute> {
        TvSearchScreen(onOpenItem = onOpenItem)
    }
}

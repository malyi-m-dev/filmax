package com.filmax.feature.search.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.search.SearchScreen
import kotlinx.serialization.Serializable

@Serializable
object SearchRoute

fun NavGraphBuilder.searchScreen(onOpenItem: (Int) -> Unit) {
    composable<SearchRoute> {
        SearchScreen(onOpenItem = onOpenItem)
    }
}

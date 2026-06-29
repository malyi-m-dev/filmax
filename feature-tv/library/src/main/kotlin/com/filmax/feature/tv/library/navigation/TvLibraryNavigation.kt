package com.filmax.feature.tv.library.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.tv.library.TvLibraryScreen
import kotlinx.serialization.Serializable

@Serializable
object TvLibraryRoute

fun NavGraphBuilder.tvLibraryScreen(onOpenItem: (Int) -> Unit) {
    composable<TvLibraryRoute> {
        TvLibraryScreen(onOpenItem = onOpenItem)
    }
}

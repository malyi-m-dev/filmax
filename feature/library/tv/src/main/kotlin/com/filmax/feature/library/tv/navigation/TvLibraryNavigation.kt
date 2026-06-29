package com.filmax.feature.library.tv.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.library.tv.TvLibraryScreen
import kotlinx.serialization.Serializable

@Serializable
object TvLibraryRoute

fun NavGraphBuilder.tvLibraryScreen(onOpenItem: (Int) -> Unit) {
    composable<TvLibraryRoute> {
        TvLibraryScreen(onOpenItem = onOpenItem)
    }
}

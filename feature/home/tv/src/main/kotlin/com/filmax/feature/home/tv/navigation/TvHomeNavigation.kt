package com.filmax.feature.home.tv.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.home.tv.TvHomeScreen
import kotlinx.serialization.Serializable

@Serializable
object TvHomeRoute

fun NavGraphBuilder.tvHomeScreen(
    onOpenItem: (Int) -> Unit,
    onPlay: (itemId: Int, videoId: Int) -> Unit,
    onOpenCollection: (id: Int, title: String) -> Unit,
) {
    composable<TvHomeRoute> {
        TvHomeScreen(
            onOpenItem = onOpenItem,
            onPlay = onPlay,
            onOpenCollection = onOpenCollection,
        )
    }
}

package com.filmax.feature.details.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.details.DetailsScreen
import kotlinx.serialization.Serializable

@Serializable
data class DetailsRoute(val itemId: Int)

fun NavGraphBuilder.detailsScreen(
    onBack: () -> Unit,
    onPlay: (itemId: Int) -> Unit,
    onOpenItem: (Int) -> Unit,
) {
    composable<DetailsRoute> {
        DetailsScreen(onBack = onBack, onPlay = onPlay, onOpenItem = onOpenItem)
    }
}

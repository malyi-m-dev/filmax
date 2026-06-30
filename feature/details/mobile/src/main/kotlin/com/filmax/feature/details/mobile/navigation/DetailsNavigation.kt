package com.filmax.feature.details.mobile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.details.mobile.DetailsScreen
import com.filmax.feature.details.common.navigation.DetailsRoute

fun NavGraphBuilder.detailsScreen(
    onBack: () -> Unit,
    onPlay: (itemId: Int) -> Unit,
    onOpenItem: (Int) -> Unit,
) {
    composable<DetailsRoute> {
        DetailsScreen(onBack = onBack, onPlay = onPlay, onOpenItem = onOpenItem)
    }
}

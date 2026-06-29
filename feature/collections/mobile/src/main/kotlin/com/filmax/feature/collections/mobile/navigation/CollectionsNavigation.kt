package com.filmax.feature.collections.mobile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.filmax.feature.collections.mobile.CollectionDetailScreen
import com.filmax.feature.collections.mobile.CollectionsScreen
import com.filmax.feature.collections.navigation.CollectionDetailRoute
import com.filmax.feature.collections.navigation.CollectionsRoute

fun NavGraphBuilder.collectionsScreen(onOpenCollection: (Int, String) -> Unit) {
    composable<CollectionsRoute> {
        CollectionsScreen(onOpenCollection = onOpenCollection)
    }
}

fun NavGraphBuilder.collectionDetailScreen(
    onBack: () -> Unit,
    onOpenItem: (Int) -> Unit,
) {
    composable<CollectionDetailRoute> { entry ->
        val route = entry.toRoute<CollectionDetailRoute>()
        CollectionDetailScreen(title = route.title, onBack = onBack, onOpenItem = onOpenItem)
    }
}

package com.filmax.feature.collections.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.filmax.feature.collections.CollectionDetailScreen
import com.filmax.feature.collections.CollectionsScreen
import kotlinx.serialization.Serializable

@Serializable
object CollectionsRoute

@Serializable
data class CollectionDetailRoute(val collectionId: Int, val title: String)

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

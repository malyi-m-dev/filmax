package com.filmax.feature.collections.tv.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.filmax.feature.collections.common.navigation.CollectionDetailRoute
import com.filmax.feature.collections.tv.TvCollectionDetailScreen
import com.filmax.feature.collections.tv.TvCollectionsScreen
import kotlinx.serialization.Serializable

/** TV-таб «Подборки». */
@Serializable
object TvCollectionsRoute

fun NavGraphBuilder.tvCollectionsScreen(onOpenCollection: (Int, String) -> Unit) {
    composable<TvCollectionsRoute> {
        TvCollectionsScreen(onOpenCollection = onOpenCollection)
    }
}

/** Экран одной подборки — на общем [CollectionDetailRoute] (его читает CollectionDetailScreenModel). */
fun NavGraphBuilder.tvCollectionDetailScreen(onOpenItem: (Int) -> Unit) {
    composable<CollectionDetailRoute> { entry ->
        val route = entry.toRoute<CollectionDetailRoute>()
        TvCollectionDetailScreen(title = route.title, onOpenItem = onOpenItem)
    }
}

package com.filmax.feature.collections.tv.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.filmax.feature.collections.common.navigation.CollectionDetailRoute
import com.filmax.feature.collections.tv.TvCollectionDetailScreen

/**
 * Экран одной подборки — на общем [CollectionDetailRoute] (его читает CollectionDetailScreenModel).
 *
 * Экрана-списка подборок здесь нет: вкладкой «Подборки» быть перестали и живут содержимым
 * Каталога, откуда и открывается конкретная подборка.
 */
fun NavGraphBuilder.tvCollectionDetailScreen() {
    composable<CollectionDetailRoute> { entry ->
        val route = entry.toRoute<CollectionDetailRoute>()
        TvCollectionDetailScreen(collectionId = route.collectionId, title = route.title)
    }
}

package com.filmax.feature.collections.mobile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.collections.common.navigation.CollectionDetailRoute
import com.filmax.feature.collections.mobile.CollectionDetailScreen

fun NavGraphBuilder.collectionDetailScreen() {
    composable<CollectionDetailRoute> { CollectionDetailScreen() }
}

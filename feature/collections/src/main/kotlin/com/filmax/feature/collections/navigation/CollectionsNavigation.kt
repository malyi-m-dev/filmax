package com.filmax.feature.collections.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.collections.CollectionsScreen
import kotlinx.serialization.Serializable

@Serializable
object CollectionsRoute

fun NavGraphBuilder.collectionsScreen(onOpenItem: (Int) -> Unit) {
    composable<CollectionsRoute> {
        CollectionsScreen(onOpenItem = onOpenItem)
    }
}

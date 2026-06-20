package com.filmax.feature.categories.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.categories.CategoriesScreen
import kotlinx.serialization.Serializable

@Serializable
object CategoriesRoute

fun NavGraphBuilder.categoriesScreen(onOpenItem: (Int) -> Unit) {
    composable<CategoriesRoute> {
        CategoriesScreen(onOpenItem = onOpenItem)
    }
}

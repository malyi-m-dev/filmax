package com.filmax.feature.search.mobile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.search.mobile.CatalogScreen
import kotlinx.serialization.Serializable

/** Маршрут вкладки «Каталог». Имя прежнее: поиск уехал внутрь каталога, а не наоборот. */
@Serializable
object SearchRoute

fun NavGraphBuilder.searchScreen(onOpenItem: (Int) -> Unit) {
    composable<SearchRoute> {
        CatalogScreen(onOpenItem = onOpenItem)
    }
}

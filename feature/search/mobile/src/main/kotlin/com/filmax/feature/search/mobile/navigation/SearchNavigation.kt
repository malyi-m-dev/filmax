package com.filmax.feature.search.mobile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.filmax.core.domain.catalog.CatalogSection
import com.filmax.feature.search.common.navigation.BrowseRoute
import com.filmax.feature.search.mobile.CatalogScreen
import com.filmax.feature.search.mobile.SearchScreen
import kotlinx.serialization.Serializable

/** Маршрут вкладки «Каталог». */
@Serializable
object CatalogRoute

/** Маршрут вкладки «Поиск». */
@Serializable
object SearchRoute

fun NavGraphBuilder.catalogScreen() {
    composable<CatalogRoute> {
        CatalogScreen()
    }
}

fun NavGraphBuilder.searchScreen() {
    composable<SearchRoute> {
        SearchScreen()
    }
}

/** Раздел каталога поверх раздела: тот же Каталог, но с выборкой раздела и кнопкой «назад». */
fun NavGraphBuilder.browseScreen() {
    composable<BrowseRoute> { entry ->
        CatalogScreen(section = CatalogSection.from(entry.toRoute<BrowseRoute>().section))
    }
}

package com.filmax.feature.search.mobile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
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

/** Раздел каталога поверх вкладки: тот же Каталог; выборку, заголовок и «назад» раздел задаёт модели через маршрут. */
fun NavGraphBuilder.browseScreen() {
    composable<BrowseRoute> {
        CatalogScreen()
    }
}

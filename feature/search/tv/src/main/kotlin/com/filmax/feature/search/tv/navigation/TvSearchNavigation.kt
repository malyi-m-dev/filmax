package com.filmax.feature.search.tv.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.search.tv.TvCatalogScreen
import kotlinx.serialization.Serializable

/**
 * Маршрут вкладки «Каталог». Имя оставлено прежним: на него ссылается таб-бар в `:app`, а
 * поиск никуда не делся — он стал одним из фильтров каталога, а не отдельным экраном.
 */
@Serializable
object TvSearchRoute

fun NavGraphBuilder.tvSearchScreen(onOpenItem: (Int) -> Unit) {
    composable<TvSearchRoute> {
        TvCatalogScreen(onOpenItem = onOpenItem)
    }
}

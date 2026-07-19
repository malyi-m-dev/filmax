package com.filmax.feature.library.mobile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.library.mobile.LibraryScreen
import kotlinx.serialization.Serializable

@Serializable
object LibraryRoute

/**
 * Раздел «Моё». Все карточки — включая «Продолжить» и «Историю» — ведут в карточку тайтла
 * через [onOpenItem]. [onOpenCatalog] — единственное действие пустых состояний раздела.
 */
fun NavGraphBuilder.libraryScreen(
    onOpenItem: (Int) -> Unit,
    onOpenCatalog: () -> Unit,
) {
    composable<LibraryRoute> {
        LibraryScreen(
            onOpenItem = onOpenItem,
            onOpenCatalog = onOpenCatalog,
        )
    }
}

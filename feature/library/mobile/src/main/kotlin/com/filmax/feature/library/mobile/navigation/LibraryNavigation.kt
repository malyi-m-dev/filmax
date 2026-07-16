package com.filmax.feature.library.mobile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.library.mobile.LibraryScreen
import kotlinx.serialization.Serializable

@Serializable
object LibraryRoute

/**
 * Раздел «Моё». [onPlay] обязателен: «Продолжить» и «История» ведут сразу в плеер (videoId
 * из истории, -1 — если трек единственный), постеры «Буду смотреть» и содержимое папок — в
 * детали. [onOpenCatalog] — единственное действие пустых состояний раздела.
 */
fun NavGraphBuilder.libraryScreen(
    onOpenItem: (Int) -> Unit,
    onPlay: (itemId: Int, videoId: Int) -> Unit,
    onOpenCatalog: () -> Unit,
) {
    composable<LibraryRoute> {
        LibraryScreen(
            onOpenItem = onOpenItem,
            onPlay = onPlay,
            onOpenCatalog = onOpenCatalog,
        )
    }
}

package com.filmax.feature.library.tv.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.library.tv.TvLibraryScreen
import kotlinx.serialization.Serializable

@Serializable
object TvLibraryRoute

/**
 * Раздел «Моё». Все карточки — «Продолжить», «История», «Буду смотреть» и содержимое папок —
 * ведут в карточку тайтла: там есть и «Продолжить · SxEy», и выбор серий, и описание.
 */
fun NavGraphBuilder.tvLibraryScreen(
    onOpenItem: (Int) -> Unit,
) {
    composable<TvLibraryRoute> {
        TvLibraryScreen(onOpenItem = onOpenItem)
    }
}

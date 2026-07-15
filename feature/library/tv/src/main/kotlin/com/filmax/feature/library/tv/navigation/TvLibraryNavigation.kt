package com.filmax.feature.library.tv.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.library.tv.TvLibraryScreen
import kotlinx.serialization.Serializable

@Serializable
object TvLibraryRoute

/**
 * Раздел «Моё». [onPlay] обязателен: «Продолжить» и «История» ведут сразу в плеер (videoId
 * из истории, -1 — если трек единственный), постеры «Буду смотреть» — в детали.
 */
fun NavGraphBuilder.tvLibraryScreen(
    onOpenItem: (Int) -> Unit,
    onPlay: (itemId: Int, videoId: Int) -> Unit,
) {
    composable<TvLibraryRoute> {
        TvLibraryScreen(onOpenItem = onOpenItem, onPlay = onPlay)
    }
}

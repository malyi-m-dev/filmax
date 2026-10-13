package com.filmax.feature.library.tv.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.library.common.navigation.FolderRoute
import com.filmax.feature.library.tv.TvFolderScreen
import com.filmax.feature.library.tv.TvMineScreen
import kotlinx.serialization.Serializable

@Serializable
object TvLibraryRoute

/** Раздел «Моё»: сегменты «Продолжить · Буду смотреть · Папки · История». */
fun NavGraphBuilder.tvLibraryScreen() {
    composable<TvLibraryRoute> {
        TvMineScreen()
    }
}

/** Открытая папка-закладка — push-экран без меню; маршрут общий с моделью (FolderRoute). */
fun NavGraphBuilder.tvFolderScreen() {
    composable<FolderRoute> {
        TvFolderScreen()
    }
}

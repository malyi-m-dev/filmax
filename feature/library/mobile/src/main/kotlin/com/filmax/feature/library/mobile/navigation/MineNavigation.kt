package com.filmax.feature.library.mobile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.library.common.navigation.FolderRoute
import com.filmax.feature.library.mobile.FolderScreen
import com.filmax.feature.library.mobile.MineScreen
import kotlinx.serialization.Serializable

/** Маршрут вкладки «Моё». */
@Serializable
object MineRoute

/** Раздел «Моё». */
fun NavGraphBuilder.mineScreen() {
    composable<MineRoute> {
        MineScreen()
    }
}

/** Открытая папка-закладка — экран поверх «Моё»; маршрут общий с ТВ, его читает FolderScreenModel. */
fun NavGraphBuilder.folderScreen() {
    composable<FolderRoute> {
        FolderScreen()
    }
}

package com.filmax.feature.library.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.library.LibraryScreen
import kotlinx.serialization.Serializable

@Serializable
object LibraryRoute

fun NavGraphBuilder.libraryScreen(onOpenItem: (Int) -> Unit) {
    composable<LibraryRoute> {
        LibraryScreen(onOpenItem = onOpenItem)
    }
}

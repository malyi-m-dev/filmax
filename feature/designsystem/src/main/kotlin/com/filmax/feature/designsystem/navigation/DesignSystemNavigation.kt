package com.filmax.feature.designsystem.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.designsystem.DesignSystemScreen
import kotlinx.serialization.Serializable

@Serializable
object DesignSystemRoute

fun NavGraphBuilder.designSystemScreen(onBack: () -> Unit) {
    composable<DesignSystemRoute> {
        DesignSystemScreen(onBack = onBack)
    }
}

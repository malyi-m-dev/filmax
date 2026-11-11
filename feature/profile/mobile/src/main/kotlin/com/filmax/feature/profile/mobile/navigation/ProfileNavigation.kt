package com.filmax.feature.profile.mobile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.profile.mobile.ProfileScreen
import kotlinx.serialization.Serializable

@Serializable
object ProfileRoute

fun NavGraphBuilder.profileScreen(
    onCheckUpdates: () -> Unit,
    showDesignSystem: Boolean = false,
) {
    composable<ProfileRoute> {
        ProfileScreen(
            onCheckUpdates = onCheckUpdates,
            showDesignSystem = showDesignSystem,
        )
    }
}

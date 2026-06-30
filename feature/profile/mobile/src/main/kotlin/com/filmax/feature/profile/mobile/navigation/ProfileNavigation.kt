package com.filmax.feature.profile.mobile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.profile.mobile.ProfileScreen
import kotlinx.serialization.Serializable

@Serializable
object ProfileRoute

fun NavGraphBuilder.profileScreen(
    onLogout: () -> Unit,
    onOpenDesignSystem: (() -> Unit)? = null,
) {
    composable<ProfileRoute> {
        ProfileScreen(
            onLogout = onLogout,
            onOpenDesignSystem = onOpenDesignSystem,
        )
    }
}

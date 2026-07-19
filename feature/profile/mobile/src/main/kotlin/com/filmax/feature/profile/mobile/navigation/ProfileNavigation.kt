package com.filmax.feature.profile.mobile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.profile.mobile.DeviceSettingsScreen
import com.filmax.feature.profile.mobile.ProfileScreen
import kotlinx.serialization.Serializable

@Serializable
object ProfileRoute

/** Push-экран «Настройки устройства» (открывается из Профиля). */
@Serializable
object DeviceSettingsRoute

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

fun NavGraphBuilder.deviceSettingsScreen(onBack: () -> Unit) {
    composable<DeviceSettingsRoute> {
        DeviceSettingsScreen(onBack = onBack)
    }
}

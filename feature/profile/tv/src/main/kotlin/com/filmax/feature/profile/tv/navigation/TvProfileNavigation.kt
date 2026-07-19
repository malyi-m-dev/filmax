package com.filmax.feature.profile.tv.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.profile.tv.TvDeviceSettingsScreen
import com.filmax.feature.profile.tv.TvProfileScreen
import kotlinx.serialization.Serializable

@Serializable
object TvProfileRoute

/** Push-экран «Настройки устройства» (открывается из TV-Профиля). */
@Serializable
object TvDeviceSettingsRoute

fun NavGraphBuilder.tvProfileScreen(
    onLogout: () -> Unit,
) {
    composable<TvProfileRoute> {
        TvProfileScreen(
            onLogout = onLogout,
        )
    }
}

fun NavGraphBuilder.tvDeviceSettingsScreen(onBack: () -> Unit) {
    composable<TvDeviceSettingsRoute> {
        TvDeviceSettingsScreen(onBack = onBack)
    }
}

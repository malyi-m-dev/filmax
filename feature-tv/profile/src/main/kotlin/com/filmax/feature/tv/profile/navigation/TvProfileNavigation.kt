package com.filmax.feature.tv.profile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.tv.profile.TvProfileScreen
import kotlinx.serialization.Serializable

@Serializable
object TvProfileRoute

fun NavGraphBuilder.tvProfileScreen(onLogout: () -> Unit) {
    composable<TvProfileRoute> {
        TvProfileScreen(onLogout = onLogout)
    }
}

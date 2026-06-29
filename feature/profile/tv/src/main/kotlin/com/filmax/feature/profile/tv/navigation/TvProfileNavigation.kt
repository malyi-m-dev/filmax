package com.filmax.feature.profile.tv.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.profile.tv.TvProfileScreen
import kotlinx.serialization.Serializable

@Serializable
object TvProfileRoute

fun NavGraphBuilder.tvProfileScreen(onLogout: () -> Unit) {
    composable<TvProfileRoute> {
        TvProfileScreen(onLogout = onLogout)
    }
}

package com.filmax.feature.profile.tv.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.filmax.feature.profile.tv.TvProfileScreen
import kotlinx.serialization.Serializable

@Serializable
object TvProfileRoute

/** [remoteCard] — карточка «Пульт с телефона»: её даёт граф, профиль о фиче связи не знает. */
fun NavGraphBuilder.tvProfileScreen(
    onCheckUpdates: () -> Unit,
    remoteCard: @Composable (Modifier) -> Unit = {},
) {
    composable<TvProfileRoute> {
        TvProfileScreen(
            onCheckUpdates = onCheckUpdates,
            remoteCard = remoteCard,
        )
    }
}

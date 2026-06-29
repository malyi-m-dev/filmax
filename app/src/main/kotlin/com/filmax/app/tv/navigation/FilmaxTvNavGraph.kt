package com.filmax.app.tv.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.filmax.feature.details.navigation.DetailsRoute
import com.filmax.feature.player.navigation.PlayerRoute
import com.filmax.feature.collections.navigation.CollectionDetailRoute
import com.filmax.feature.collections.tv.navigation.tvCollectionDetailScreen
import com.filmax.feature.collections.tv.navigation.tvCollectionsScreen
import com.filmax.feature.details.tv.navigation.tvDetailsScreen
import com.filmax.feature.home.tv.navigation.TvHomeRoute
import com.filmax.feature.home.tv.navigation.tvHomeScreen
import com.filmax.feature.library.tv.navigation.tvLibraryScreen
import com.filmax.feature.onboarding.tv.navigation.TvOnboardingRoute
import com.filmax.feature.onboarding.tv.navigation.tvOnboardingScreen
import com.filmax.feature.player.tv.navigation.tvPlayerScreen
import com.filmax.feature.profile.tv.navigation.tvProfileScreen
import com.filmax.feature.search.tv.navigation.tvSearchScreen
import com.filmax.app.navigation.RootScreenModel
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
private object TvSplashRoute

@Composable
fun FilmaxTvNavGraph(
    // Переиспользуем общий RootScreenModel (тот же auth.isAuthenticated, что и в телефонном графе).
    rootScreenModel: RootScreenModel = koinViewModel(),
) {
    val rootState by rootScreenModel.collectAsState()
    val isAuthenticated = rootState.isAuthenticated
    val navController = rememberNavController()

    LaunchedEffect(isAuthenticated) {
        val auth = isAuthenticated ?: return@LaunchedEffect
        if (auth) {
            navController.navigate(TvHomeRoute) { popUpTo(TvSplashRoute) { inclusive = true } }
        } else {
            navController.navigate(TvOnboardingRoute) { popUpTo(TvSplashRoute) { inclusive = true } }
        }
    }

    val backStack by navController.currentBackStackEntryAsState()
    val currentDest = backStack?.destination
    val showTopBar = TOP_LEVEL_ROUTES.any { currentDest?.hasRoute(it) == true }

    // Переход по вкладкам: единственный экземпляр + сохранение/восстановление состояния.
    fun navigateTab(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        NavHost(
            navController = navController,
            startDestination = TvSplashRoute,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() },
        ) {
            composable<TvSplashRoute> { Box(Modifier.fillMaxSize()) }

            tvOnboardingScreen(
                onAuthenticated = {
                    navController.navigate(TvHomeRoute) { popUpTo(TvOnboardingRoute) { inclusive = true } }
                },
            )

            tvHomeScreen(onOpenItem = { navController.navigate(DetailsRoute(it)) })
            tvSearchScreen(onOpenItem = { navController.navigate(DetailsRoute(it)) })
            tvCollectionsScreen(
                onOpenCollection = { id, title ->
                    navController.navigate(CollectionDetailRoute(collectionId = id, title = title))
                },
            )
            tvCollectionDetailScreen(onOpenItem = { navController.navigate(DetailsRoute(it)) })
            tvLibraryScreen(onOpenItem = { navController.navigate(DetailsRoute(it)) })
            tvProfileScreen(
                onLogout = {
                    navController.navigate(TvOnboardingRoute) { popUpTo(TvHomeRoute) { inclusive = true } }
                },
            )

            tvDetailsScreen(
                onPlay = { navController.navigate(PlayerRoute(it)) },
                onOpenItem = { navController.navigate(DetailsRoute(it)) },
            )
            tvPlayerScreen(onBack = { navController.popBackStack() })
        }

        if (showTopBar) {
            TvTopNavBar(
                currentDestination = currentDest,
                onSelectTab = { navigateTab(it) },
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

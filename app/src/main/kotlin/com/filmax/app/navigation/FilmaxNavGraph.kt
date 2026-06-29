package com.filmax.app.navigation

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
import org.koin.androidx.compose.koinViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.filmax.app.BuildConfig
import com.filmax.core.ui.components.FilmaxTab
import com.filmax.core.ui.components.FilmaxTabBar
import com.filmax.feature.collections.navigation.CollectionDetailRoute
import com.filmax.feature.collections.navigation.CollectionsRoute
import com.filmax.feature.designsystem.navigation.DesignSystemRoute
import com.filmax.feature.designsystem.navigation.designSystemScreen
import com.filmax.feature.collections.navigation.collectionDetailScreen
import com.filmax.feature.collections.navigation.collectionsScreen
import com.filmax.feature.details.navigation.DetailsRoute
import com.filmax.feature.details.mobile.navigation.detailsScreen
import com.filmax.feature.home.mobile.navigation.HomeRoute
import com.filmax.feature.home.mobile.navigation.homeScreen
import com.filmax.feature.library.mobile.navigation.LibraryRoute
import com.filmax.feature.library.mobile.navigation.libraryScreen
import com.filmax.feature.onboarding.mobile.navigation.OnboardingRoute
import com.filmax.feature.onboarding.mobile.navigation.onboardingScreen
import com.filmax.feature.player.navigation.PlayerRoute
import com.filmax.feature.player.mobile.navigation.playerScreen
import com.filmax.feature.profile.mobile.navigation.ProfileRoute
import com.filmax.feature.profile.mobile.navigation.profileScreen
import com.filmax.feature.search.mobile.navigation.SearchRoute
import com.filmax.feature.search.mobile.navigation.searchScreen
import kotlinx.serialization.Serializable

@Serializable
private object SplashRoute

private val tabRouteClasses = mapOf(
    FilmaxTab.HOME to HomeRoute::class,
    FilmaxTab.SEARCH to SearchRoute::class,
    FilmaxTab.COLLECTIONS to CollectionsRoute::class,
    FilmaxTab.LIBRARY to LibraryRoute::class,
    FilmaxTab.PROFILE to ProfileRoute::class,
)

@Composable
fun FilmaxNavGraph(
    rootScreenModel: RootScreenModel = koinViewModel(),
) {
    val rootState by rootScreenModel.collectAsState()
    val isAuthenticated = rootState.isAuthenticated
    val navController = rememberNavController()

    LaunchedEffect(isAuthenticated) {
        val auth = isAuthenticated ?: return@LaunchedEffect
        if (auth) {
            navController.navigate(HomeRoute) {
                popUpTo(SplashRoute) { inclusive = true }
            }
        } else {
            navController.navigate(OnboardingRoute) {
                popUpTo(SplashRoute) { inclusive = true }
            }
        }
    }

    val backStack by navController.currentBackStackEntryAsState()
    val currentDest = backStack?.destination

    val bottomBarTabs = listOf(
        HomeRoute::class,
        SearchRoute::class,
        CollectionsRoute::class,
        LibraryRoute::class,
        ProfileRoute::class,
    )
    val showBottomBar = bottomBarTabs.any { currentDest?.hasRoute(it) == true }

    val selectedTab = tabRouteClasses.entries
        .firstOrNull { (_, routeClass) -> currentDest?.hasRoute(routeClass) == true }
        ?.key
        ?: FilmaxTab.HOME

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        NavHost(
            navController = navController,
            startDestination = SplashRoute,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() },
        ) {
            composable<SplashRoute> {
                Box(Modifier.fillMaxSize())
            }

            onboardingScreen(
                onAuthenticated = {
                    navController.navigate(HomeRoute) {
                        popUpTo(OnboardingRoute) { inclusive = true }
                    }
                },
            )

            homeScreen(
                onOpenItem = { navController.navigate(DetailsRoute(it)) },
            )
            searchScreen(
                onOpenItem = { navController.navigate(DetailsRoute(it)) },
            )
            collectionsScreen(
                onOpenCollection = { id, title ->
                    navController.navigate(CollectionDetailRoute(collectionId = id, title = title))
                },
            )
            collectionDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenItem = { navController.navigate(DetailsRoute(it)) },
            )
            libraryScreen(
                onOpenItem = { navController.navigate(DetailsRoute(it)) },
            )
            profileScreen(
                onLogout = {
                    navController.navigate(OnboardingRoute) {
                        popUpTo(HomeRoute) { inclusive = true }
                    }
                },
                onOpenDesignSystem = if (BuildConfig.DEBUG) {
                    { navController.navigate(DesignSystemRoute) }
                } else {
                    null
                },
            )

            detailsScreen(
                onBack = { navController.popBackStack() },
                onPlay = { navController.navigate(PlayerRoute(it)) },
                onOpenItem = { navController.navigate(DetailsRoute(it)) },
            )
            playerScreen(
                onBack = { navController.popBackStack() },
            )
            designSystemScreen(
                onBack = { navController.popBackStack() },
            )
        }

        if (showBottomBar) {
            FilmaxTabBar(
                selected = selectedTab,
                onSelect = { tab ->
                    val route: Any = when (tab) {
                        FilmaxTab.HOME -> HomeRoute
                        FilmaxTab.SEARCH -> SearchRoute
                        FilmaxTab.COLLECTIONS -> CollectionsRoute
                        FilmaxTab.LIBRARY -> LibraryRoute
                        FilmaxTab.PROFILE -> ProfileRoute
                    }
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

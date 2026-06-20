package com.filmax.app.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.filmax.core.ui.components.FilmaxTab
import com.filmax.core.ui.components.FilmaxTabBar
import com.filmax.feature.categories.navigation.CategoriesRoute
import com.filmax.feature.categories.navigation.categoriesScreen
import com.filmax.feature.details.navigation.DetailsRoute
import com.filmax.feature.details.navigation.detailsScreen
import com.filmax.feature.home.navigation.HomeRoute
import com.filmax.feature.home.navigation.homeScreen
import com.filmax.feature.library.navigation.LibraryRoute
import com.filmax.feature.library.navigation.libraryScreen
import com.filmax.feature.onboarding.navigation.OnboardingRoute
import com.filmax.feature.onboarding.navigation.onboardingScreen
import com.filmax.feature.player.navigation.PlayerRoute
import com.filmax.feature.player.navigation.playerScreen
import com.filmax.feature.profile.navigation.ProfileRoute
import com.filmax.feature.profile.navigation.profileScreen
import com.filmax.feature.search.navigation.SearchRoute
import com.filmax.feature.search.navigation.searchScreen
import kotlinx.serialization.Serializable

@Serializable
private object SplashRoute

private val tabRouteClasses = mapOf(
    FilmaxTab.HOME to HomeRoute::class,
    FilmaxTab.SEARCH to SearchRoute::class,
    FilmaxTab.CATEGORIES to CategoriesRoute::class,
    FilmaxTab.LIBRARY to LibraryRoute::class,
    FilmaxTab.PROFILE to ProfileRoute::class,
)

@Composable
fun FilmaxNavGraph(
    rootViewModel: RootViewModel = hiltViewModel(),
) {
    val isAuthenticated by rootViewModel.isAuthenticated.collectAsStateWithLifecycle(null)
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
        CategoriesRoute::class,
        LibraryRoute::class,
        ProfileRoute::class,
    )
    val showBottomBar = bottomBarTabs.any { currentDest?.hasRoute(it) == true }

    val selectedTab = tabRouteClasses.entries
        .firstOrNull { (_, routeClass) -> currentDest?.hasRoute(routeClass) == true }
        ?.key
        ?: FilmaxTab.HOME

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                FilmaxTabBar(
                    selected = selectedTab,
                    onSelect = { tab ->
                        val route: Any = when (tab) {
                            FilmaxTab.HOME -> HomeRoute
                            FilmaxTab.SEARCH -> SearchRoute
                            FilmaxTab.CATEGORIES -> CategoriesRoute
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
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = SplashRoute,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
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
            categoriesScreen(
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
            )

            detailsScreen(
                onBack = { navController.popBackStack() },
                onPlay = { navController.navigate(PlayerRoute(it)) },
                onOpenItem = { navController.navigate(DetailsRoute(it)) },
            )
            playerScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}

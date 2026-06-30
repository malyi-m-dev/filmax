package com.filmax.app.tv.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import com.filmax.core.tv.designsystem.LocalTvScrollToTop
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.filmax.feature.details.common.navigation.DetailsRoute
import com.filmax.feature.player.common.navigation.PlayerRoute
import com.filmax.feature.collections.common.navigation.CollectionDetailRoute
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

    // Явная связь фокуса между оверлейной шапкой и контентом: они — соседи в Box, и
    // D-pad-поиск между ними сам по себе не проходит, поэтому шапка по «вниз» уводит в
    // [contentFocus], а контент получает стартовый фокус, чтобы экран сразу скроллился.
    val navBarFocus = remember { FocusRequester() }
    val contentFocus = remember { FocusRequester() }

    // Сигнал «контент — в начало»: растёт при каждом заходе фокуса в шапку, экраны слушают его
    // через LocalTvScrollToTop и скроллят свой контейнер вверх, чтобы он не «застревал» внизу.
    var scrollToTopSignal by remember { mutableIntStateOf(0) }

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
        CompositionLocalProvider(LocalTvScrollToTop provides scrollToTopSignal) {
        NavHost(
            navController = navController,
            startDestination = TvSplashRoute,
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(contentFocus)
                .focusProperties { up = navBarFocus }
                .focusGroup(),
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
                onPlay = { itemId, videoId -> navController.navigate(PlayerRoute(itemId, videoId)) },
                onOpenItem = { navController.navigate(DetailsRoute(it)) },
            )
            tvPlayerScreen(onBack = { navController.popBackStack() })
        }
        }

        if (showTopBar) {
            TvTopNavBar(
                currentDestination = currentDest,
                onSelectTab = { navigateTab(it) },
                navBarFocus = navBarFocus,
                contentFocus = contentFocus,
                initials = rootState.initials,
                // Любой заход фокуса в шапку (с контента или стартовый) — повод увести контент вверх.
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .onFocusChanged { if (it.hasFocus) scrollToTopSignal++ },
            )
        }
    }

    // При первом входе в top-level отдаём фокус таб-бару (его активной вкладке). Не
    // перезапрашиваем фокус при каждой смене вкладки: иначе переброс на ещё не готовый
    // контент срывается, и фокус откатывается на первую вкладку («Главная»). Вниз к
    // контенту пользователь уходит «вниз» (down=contentFocus на вкладках).
    LaunchedEffect(showTopBar) {
        if (showTopBar) runCatching { navBarFocus.requestFocus() }
    }
}

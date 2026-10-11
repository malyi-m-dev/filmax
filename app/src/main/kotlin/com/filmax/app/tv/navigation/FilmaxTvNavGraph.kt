package com.filmax.app.tv.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.filmax.app.navigation.AuthStateNavigation
import com.filmax.app.navigation.NavigationCommands
import com.filmax.app.navigation.RootScreenModel
import com.filmax.app.navigation.navFadeIn
import com.filmax.app.navigation.navFadeOut
import com.filmax.core.navigation.Navigator
import com.filmax.core.tv.designsystem.LocalTvNavBarFocused
import com.filmax.core.tv.designsystem.LocalTvScrollToTop
import com.filmax.feature.collections.tv.navigation.tvCollectionDetailScreen
import com.filmax.feature.details.tv.navigation.tvDetailsScreen
import com.filmax.feature.home.tv.navigation.TvHomeRoute
import com.filmax.feature.home.tv.navigation.tvHomeScreen
import com.filmax.feature.library.tv.navigation.tvLibraryScreen
import com.filmax.feature.onboarding.tv.navigation.TvOnboardingRoute
import com.filmax.feature.onboarding.tv.navigation.tvOnboardingScreen
import com.filmax.feature.player.tv.navigation.tvPlayerScreen
import com.filmax.feature.player.tv.navigation.tvTrailerScreen
import com.filmax.feature.profile.tv.navigation.tvDeviceSettingsScreen
import com.filmax.feature.profile.tv.navigation.tvProfileScreen
import com.filmax.feature.search.tv.navigation.tvFilmographyScreen
import com.filmax.feature.search.tv.navigation.tvSearchScreen
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Serializable
private object TvSplashRoute

@Composable
fun FilmaxTvNavGraph(
    onCheckUpdates: () -> Unit,
    // Переиспользуем общий RootScreenModel (тот же auth.isAuthenticated, что и в телефонном графе).
    rootScreenModel: RootScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val rootState by rootScreenModel.collectAsState()
    val isAuthenticated = rootState.isAuthenticated
    val navController = rememberNavController()

    NavigationCommands(navController, TvRoutes)

    AuthStateNavigation(
        isAuthenticated = isAuthenticated,
        navController = navController,
        homeRoute = TvHomeRoute,
        onboardingRoute = TvOnboardingRoute,
    )

    val backStack by navController.currentBackStackEntryAsState()
    val currentDest = backStack?.destination
    val showTopBar = TOP_LEVEL_ROUTES.any { currentDest?.hasRoute(it) == true }

    // Явная связь фокуса между оверлейной шапкой и контентом: они — соседи в Box, и
    // D-pad-поиск между ними сам по себе не проходит, поэтому шапка по «вниз» уводит в
    // [contentFocus], а контент по «вверх» — в шапку.
    val navBarFocus = remember { FocusRequester() }
    val contentFocus = remember { FocusRequester() }

    // Фокус в шапке — единственная причина, по которой экран НЕ забирает стартовый фокус себе
    // (см. tvScreenFocus): пользователь выбирает вкладки подряд, и подхват фокуса контентом
    // стоил бы ему лишнего «вверх» на каждый переход.
    val navBarFocused = remember { mutableStateOf(false) }

    // Сигнал «контент — в начало»: растёт при каждом заходе фокуса в шапку, экраны слушают его
    // через LocalTvScrollToTop и скроллят свой контейнер вверх, чтобы он не «застревал» внизу.
    var scrollToTopSignal by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        CompositionLocalProvider(
            LocalTvScrollToTop provides scrollToTopSignal,
            LocalTvNavBarFocused provides navBarFocused,
        ) {
            NavHost(
                navController = navController,
                startDestination = TvSplashRoute,
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(contentFocus)
                    .focusProperties { up = navBarFocus }
                    .focusGroup(),
                enterTransition = { navFadeIn },
                exitTransition = { navFadeOut },
                popEnterTransition = { navFadeIn },
                popExitTransition = { navFadeOut },
            ) {
                tvDestinations(onCheckUpdates)
            }
        }

        if (showTopBar) {
            TvTopNavBar(
                currentDestination = currentDest,
                onSelectTab = navigator::open,
                focus = TvTopNavBarFocus(navBar = navBarFocus, content = contentFocus),
                initials = rootState.initials,
                // Любой заход фокуса в шапку (с контента или стартовый) — повод увести контент вверх.
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .onFocusChanged {
                        navBarFocused.value = it.hasFocus
                        if (it.hasFocus) scrollToTopSignal++
                    },
            )
        }
    }
}

/**
 * Регистрация всех экранов TV-графа: сплэш, онбординг, разделы и детали/плеер.
 *
 * Оглавление, а не логика: экраны колбэков не получают — куда вести, они говорят [Navigator]-у,
 * а переход по вкладкам, замену серии и выход исполняет [NavigationCommands].
 */
private fun NavGraphBuilder.tvDestinations(onCheckUpdates: () -> Unit) {
    composable<TvSplashRoute> { Box(Modifier.fillMaxSize()) }

    tvOnboardingScreen()

    tvHomeScreen()
    tvSearchScreen()
    // «Подборки» больше не вкладка — это контент внутри Каталога. Экран содержимого
    // подборки остаётся push-экраном: в него ведёт Каталог.
    tvCollectionDetailScreen()
    // Все карточки «Моё» ведут в карточку тайтла — играть оттуда: кнопка «Продолжить · SxEy».
    tvLibraryScreen()
    tvProfileScreen(onCheckUpdates = onCheckUpdates)
    // Экран настроек устройства остаётся в графе, но из Профиля временно не открывается:
    // device/info и device/settings на бэкенде отвечают 500.
    tvDeviceSettingsScreen()

    tvDetailsScreen()
    // Фильмография человека — push-экран из деталей (тап по актёру/режиссёру).
    tvFilmographyScreen()
    tvTrailerScreen()
    tvPlayerScreen()
}

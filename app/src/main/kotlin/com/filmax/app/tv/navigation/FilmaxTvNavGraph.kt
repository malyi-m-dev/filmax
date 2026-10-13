package com.filmax.app.tv.navigation

import androidx.activity.compose.BackHandler
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
import androidx.navigation.NavHostController
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
    val showNav = TOP_LEVEL_ROUTES.any { currentDest?.hasRoute(it) == true }

    // Явная связь фокуса между оверлейным меню и контентом: они — соседи в Box, и D-pad-поиск
    // между ними сам по себе не проходит, поэтому меню по «вправо» уводит в [contentFocus], а
    // контент по «влево» с левого края — в меню.
    val navFocus = remember { FocusRequester() }
    val contentFocus = remember { FocusRequester() }

    // Пока фокус в меню, экран не забирает стартовый фокус себе (см. tvScreenFocus).
    val navFocused = remember { mutableStateOf(false) }

    // Сигнал «контент — в начало»: растёт при повторном выборе открытого раздела, экраны
    // слушают его через LocalTvScrollToTop и скроллят свой контейнер вверх.
    var scrollToTopSignal by remember { mutableIntStateOf(0) }

    // «Назад» на Главной — в меню: из глубины ленты это быстрее, чем листать к началу. Обработчик
    // регистрируется раньше всех и поэтому срабатывает последним: «назад» экранов (открытая папка
    // «Моё», режим поиска) и стек NavHost (из другого раздела — на Главную) идут первыми, а из
    // самого меню «назад» не перехватывается — выход из приложения.
    BackHandler(enabled = showNav && !navFocused.value) { navFocus.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        CompositionLocalProvider(
            LocalTvScrollToTop provides scrollToTopSignal,
            LocalTvNavBarFocused provides navFocused,
        ) {
            TvNavHost(
                navController = navController,
                onCheckUpdates = onCheckUpdates,
                modifier = Modifier
                    .focusRequester(contentFocus)
                    .focusProperties { if (showNav) left = navFocus },
            )
        }

        if (showNav) {
            TvSideNav(
                currentDestination = currentDest,
                // Повторный выбор открытого раздела — в его начало, другой раздел — переход.
                onSelect = { section, isOpen ->
                    if (isOpen) scrollToTopSignal++ else navigator.open(section)
                    contentFocus.requestFocus()
                },
                focus = TvSideNavFocus(nav = navFocus, content = contentFocus),
                initials = rootState.initials,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .onFocusChanged { navFocused.value = it.hasFocus },
            )
        }
    }
}

/** Контент разделов: NavHost со всеми экранами и общими переходами. */
@Composable
private fun TvNavHost(navController: NavHostController, onCheckUpdates: () -> Unit, modifier: Modifier) {
    NavHost(
        navController = navController,
        startDestination = TvSplashRoute,
        modifier = modifier
            .fillMaxSize()
            .focusGroup(),
        enterTransition = { navFadeIn },
        exitTransition = { navFadeOut },
        popEnterTransition = { navFadeIn },
        popExitTransition = { navFadeOut },
    ) {
        tvDestinations(onCheckUpdates)
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

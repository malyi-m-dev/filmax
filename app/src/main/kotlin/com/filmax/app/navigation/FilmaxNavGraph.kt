package com.filmax.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.filmax.app.BuildConfig
import com.filmax.core.ui.components.FilmaxTab
import com.filmax.core.ui.components.FilmaxTabBar
import com.filmax.feature.collections.common.navigation.CollectionDetailRoute
import com.filmax.feature.collections.mobile.navigation.collectionDetailScreen
import com.filmax.feature.designsystem.navigation.DesignSystemRoute
import com.filmax.feature.designsystem.navigation.designSystemScreen
import com.filmax.feature.details.common.navigation.DetailsRoute
import com.filmax.feature.details.mobile.navigation.detailsScreen
import com.filmax.feature.home.mobile.HomeActions
import com.filmax.feature.home.mobile.navigation.HomeRoute
import com.filmax.feature.home.mobile.navigation.homeScreen
import com.filmax.feature.library.mobile.navigation.LibraryRoute
import com.filmax.feature.library.mobile.navigation.libraryScreen
import com.filmax.feature.onboarding.mobile.navigation.OnboardingRoute
import com.filmax.feature.onboarding.mobile.navigation.onboardingScreen
import com.filmax.feature.player.common.navigation.PlayerRoute
import com.filmax.feature.player.common.navigation.TrailerRoute
import com.filmax.feature.player.mobile.navigation.playerScreen
import com.filmax.feature.player.mobile.navigation.trailerScreen
import com.filmax.feature.profile.mobile.navigation.ProfileRoute
import com.filmax.feature.profile.mobile.navigation.deviceSettingsScreen
import com.filmax.feature.profile.mobile.navigation.profileScreen
import com.filmax.feature.search.common.navigation.FilmographyRoute
import com.filmax.feature.search.mobile.navigation.SearchRoute
import com.filmax.feature.search.mobile.navigation.filmographyScreen
import com.filmax.feature.search.mobile.navigation.searchScreen
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
private object SplashRoute

/** Маршруты вкладок. `SearchRoute` теперь отдаёт Каталог, `LibraryRoute` — «Моё». */
private val tabRouteClasses = mapOf(
    FilmaxTab.HOME to HomeRoute::class,
    FilmaxTab.CATALOG to SearchRoute::class,
    FilmaxTab.MINE to LibraryRoute::class,
    FilmaxTab.PROFILE to ProfileRoute::class,
)

@Composable
fun FilmaxNavGraph(
    rootScreenModel: RootScreenModel = koinViewModel(),
) {
    val rootState by rootScreenModel.collectAsState()
    val isAuthenticated = rootState.isAuthenticated
    val navController = rememberNavController()

    AuthStateNavigation(
        isAuthenticated = isAuthenticated,
        navController = navController,
        homeRoute = HomeRoute,
        onboardingRoute = OnboardingRoute,
    )

    val backStack by navController.currentBackStackEntryAsState()
    val currentDest = backStack?.destination

    // Четыре раздела вместо пяти: «Поиск» уехал внутрь «Каталога» (SearchRoute отдаёт Каталог),
    // «Подборки» стали его контентом, «Библиотека» переименована в «Моё».
    val bottomBarTabs = listOf(
        HomeRoute::class,
        SearchRoute::class,
        LibraryRoute::class,
        ProfileRoute::class,
    )
    val showBottomBar = bottomBarTabs.any { currentDest?.hasRoute(it) == true }

    val selectedTab = tabRouteClasses.entries
        .firstOrNull { (_, routeClass) -> currentDest?.hasRoute(routeClass) == true }
        ?.key
        ?: FilmaxTab.HOME

    // Таб-бар живёт в Scaffold, а не оверлеем поверх контента: раньше каждый экран сам помнил
    // про padding(bottom = 120.dp) под плавающий «остров», и любой новый экран об этом забывал.
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar) {
                FilmaxTabBar(
                    selected = selectedTab,
                    onSelect = { navController.navigateToTab(it) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = SplashRoute,
            // consumeWindowInsets обязателен: без него отступ таб-бара учитывается дважды —
            // сначала здесь, потом ещё раз в imePadding() внутри экранов, и при открытой
            // клавиатуре под контентом висит пустая полоса высотой с таб-бар.
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            enterTransition = { navFadeIn },
            exitTransition = { navFadeOut },
            popEnterTransition = { navFadeIn },
            popExitTransition = { navFadeOut },
        ) {
            filmaxDestinations(navController)
        }
    }
}

/** Регистрация всех экранов телефонного графа: сплэш, онбординг, разделы и детали/плеер. */
// Линейный список регистраций destination — каждая пара «экран → колбэки навигации». Дробить
// на подфункции здесь только запутает: это оглавление графа, а не логика.
@Suppress("LongMethod")
private fun NavGraphBuilder.filmaxDestinations(navController: NavHostController) {
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
        HomeActions(
            onOpenItem = { navController.navigate(DetailsRoute(it)) },
            onPlay = { itemId, season, videoId ->
                navController.navigate(PlayerRoute(itemId, videoId, season))
            },
            onOpenCollection = { id, title ->
                navController.navigate(CollectionDetailRoute(collectionId = id, title = title))
            },
            onOpenSearch = { navController.navigateToTab(FilmaxTab.CATALOG) },
            onOpenProfile = { navController.navigateToTab(FilmaxTab.PROFILE) },
        ),
    )
    searchScreen(
        onOpenItem = { navController.navigate(DetailsRoute(it)) },
    )
    // «Подборки» больше не вкладка — это контент Каталога и ряд на Главной. Экран содержимого
    // подборки остаётся push-экраном: в него ведут они.
    collectionDetailScreen(
        onBack = { navController.popBackStack() },
        onOpenItem = { navController.navigate(DetailsRoute(it)) },
    )
    // Все карточки «Моё» ведут в карточку тайтла — играть оттуда: кнопка «Продолжить · SxEy».
    libraryScreen(
        onOpenItem = { navController.navigate(DetailsRoute(it)) },
        onOpenCatalog = { navController.navigateToTab(FilmaxTab.CATALOG) },
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
    // Экран настроек устройства остаётся в графе, но из Профиля временно не открывается:
    // device/info и device/settings на бэкенде отвечают 500.
    deviceSettingsScreen(onBack = { navController.popBackStack() })

    detailsScreen(
        onBack = { navController.popBackStack() },
        // videoId — НОМЕР видео (у фильма -1): им же kino.pub принимает и отдаёт прогресс.
        // Сезон обязателен: номер уникален только внутри сезона.
        onPlay = { itemId, season, videoId ->
            navController.navigate(PlayerRoute(itemId, videoId, season))
        },
        onOpenItem = { navController.navigate(DetailsRoute(it)) },
        onOpenPerson = { name, isDirector ->
            navController.navigate(FilmographyRoute(name = name, isDirector = isDirector))
        },
        onPlayTrailer = { url, title -> navController.navigate(TrailerRoute(url = url, title = title)) },
    )
    // Фильмография человека — push-экран из деталей (тап по актёру/режиссёру).
    filmographyScreen(
        onBack = { navController.popBackStack() },
        onOpenItem = { navController.navigate(DetailsRoute(it)) },
    )
    playerScreen(
        onBack = { navController.popBackStack() },
    )
    trailerScreen(
        onBack = { navController.popBackStack() },
    )
    designSystemScreen(
        onBack = { navController.popBackStack() },
    )
}

/**
 * Переход по нижней вкладке: единственный экземпляр + сохранение/восстановление состояния.
 *
 * Попап — до HomeRoute, реального корня вкладок: стартовый destination графа — сплэш,
 * которого после логина нет в стеке (popUpTo{inclusive}), и попап по нему молча не
 * срабатывал — каждый заход на вкладку создавал свежую энтри без восстановления состояния.
 */
private fun NavHostController.navigateToTab(tab: FilmaxTab) {
    val route: Any = when (tab) {
        FilmaxTab.HOME -> HomeRoute
        FilmaxTab.CATALOG -> SearchRoute
        FilmaxTab.MINE -> LibraryRoute
        FilmaxTab.PROFILE -> ProfileRoute
    }
    navigate(route) {
        popUpTo<HomeRoute> {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

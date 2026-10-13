package com.filmax.app.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.filmax.app.BuildConfig
import com.filmax.app.perf.ReportScreenForJank
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.ui.components.FilmaxTab
import com.filmax.core.ui.components.FilmaxTabBar
import com.filmax.feature.collections.mobile.navigation.collectionDetailScreen
import com.filmax.feature.designsystem.navigation.designSystemScreen
import com.filmax.feature.details.mobile.navigation.detailsScreen
import com.filmax.feature.home.mobile.navigation.HomeRoute
import com.filmax.feature.home.mobile.navigation.homeScreen
import com.filmax.feature.library.mobile.navigation.LibraryRoute
import com.filmax.feature.library.mobile.navigation.libraryScreen
import com.filmax.feature.onboarding.mobile.navigation.OnboardingRoute
import com.filmax.feature.onboarding.mobile.navigation.onboardingScreen
import com.filmax.feature.player.mobile.navigation.playerScreen
import com.filmax.feature.player.mobile.navigation.trailerScreen
import com.filmax.feature.profile.mobile.navigation.ProfileRoute
import com.filmax.feature.profile.mobile.navigation.deviceSettingsScreen
import com.filmax.feature.profile.mobile.navigation.profileScreen
import com.filmax.feature.search.mobile.navigation.SearchRoute
import com.filmax.feature.search.mobile.navigation.filmographyScreen
import com.filmax.feature.search.mobile.navigation.searchScreen
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Serializable
private object SplashRoute

/** Корни разделов. `SearchRoute` отдаёт Каталог, `LibraryRoute` — «Моё»: имена старше переименования. */
private val tabRoots = mapOf(
    FilmaxTab.HOME to HomeRoute::class,
    FilmaxTab.CATALOG to SearchRoute::class,
    FilmaxTab.MINE to LibraryRoute::class,
    FilmaxTab.PROFILE to ProfileRoute::class,
)

/**
 * Телефонный граф.
 *
 * Таб-бар — только на четырёх корнях разделов, как и верхняя навигация на ТВ. Карточка тайтла,
 * подборка, фильмография, плеер и трейлер идут поверх него на весь экран.
 *
 * Экраны сюда колбэков не отдают и не получают: куда вести — они говорят [Navigator]-у.
 */
@Composable
fun FilmaxNavGraph(
    onCheckUpdates: () -> Unit,
    rootScreenModel: RootScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val rootState by rootScreenModel.collectAsState()
    val navController = rememberNavController()

    NavigationCommands(navController, PhoneRoutes)

    AuthStateNavigation(
        isAuthenticated = rootState.isAuthenticated,
        navController = navController,
        homeRoute = HomeRoute,
        onboardingRoute = OnboardingRoute,
    )

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination
    ReportScreenForJank(currentDest?.route)
    val selectedTab = tabRoots.entries
        .firstOrNull { (_, root) -> currentDest?.hasRoute(root) == true }
        ?.key

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            // Бар едет вместе с контентом, а не мигает: раньше он появлялся и исчезал в кадре
            // вызова navigate(), пока экраны ещё 350 мс кроссфейдились, и вместе с ним прыгал
            // innerPadding — контент дёргался по вертикали на высоту бара посреди перехода.
            AnimatedVisibility(
                visible = selectedTab != null,
                enter = fadeIn(tween(NAV_FADE_MS)) +
                    expandVertically(tween(NAV_FADE_MS), expandFrom = Alignment.Top),
                exit = fadeOut(tween(NAV_FADE_MS)) +
                    shrinkVertically(tween(NAV_FADE_MS), shrinkTowards = Alignment.Top),
            ) {
                // Во время ухода бара selectedTab уже null — подсветку на эти 350 мс держит
                // Главная, иначе на выезде подсветка успевала мигнуть на другой раздел.
                FilmaxTabBar(
                    selected = selectedTab ?: FilmaxTab.HOME,
                    onSelect = { navigator.open(it.destination) },
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
            filmaxDestinations(onCheckUpdates)
        }
    }
}

/**
 * Регистрация всех экранов телефонного графа: сплэш, онбординг, разделы и детали/плеер.
 *
 * Оглавление, а не логика: куда ведёт каждый экран, написано в нём самом.
 */
private fun NavGraphBuilder.filmaxDestinations(onCheckUpdates: () -> Unit) {
    composable<SplashRoute> {
        Box(Modifier.fillMaxSize())
    }

    onboardingScreen()

    homeScreen()
    searchScreen()
    // «Подборки» больше не вкладка — это контент Каталога и ряд на Главной. Экран содержимого
    // подборки остаётся push-экраном: в него ведут они.
    collectionDetailScreen()
    libraryScreen()
    profileScreen(
        onCheckUpdates = onCheckUpdates,
        showDesignSystem = BuildConfig.DEBUG,
    )
    // Экран настроек устройства остаётся в графе, но из Профиля временно не открывается:
    // device/info и device/settings на бэкенде отвечают 500.
    deviceSettingsScreen()

    detailsScreen()
    // Фильмография человека — push-экран из деталей (тап по актёру/режиссёру).
    filmographyScreen()
    playerScreen()
    trailerScreen()
    designSystemScreen()
}

/** Вкладка таб-бара — тот же адрес, который называют экраны (Главная → «Каталог» и т.п.). */
private val FilmaxTab.destination: Destination
    get() = when (this) {
        FilmaxTab.HOME -> Destination.Home
        FilmaxTab.CATALOG -> Destination.Catalog
        FilmaxTab.MINE -> Destination.Mine
        FilmaxTab.PROFILE -> Destination.Profile
    }

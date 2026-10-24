package com.filmax.app.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.filmax.app.BuildConfig
import com.filmax.app.perf.ReportScreenForJank
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.GlassState
import com.filmax.core.designsystem.glassSource
import com.filmax.core.designsystem.rememberGlassState
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.ui.components.FilmaxNavBar
import com.filmax.core.ui.components.FilmaxTab
import com.filmax.core.ui.components.LocalNavBarPadding
import com.filmax.feature.collections.mobile.navigation.collectionDetailScreen
import com.filmax.feature.designsystem.navigation.designSystemScreen
import com.filmax.feature.details.mobile.navigation.detailsScreen
import com.filmax.feature.feedback.mobile.navigation.feedbackDialog
import com.filmax.feature.home.mobile.navigation.HomeRoute
import com.filmax.feature.home.mobile.navigation.homeScreen
import com.filmax.feature.library.mobile.navigation.MineRoute
import com.filmax.feature.library.mobile.navigation.folderScreen
import com.filmax.feature.library.mobile.navigation.mineScreen
import com.filmax.feature.onboarding.mobile.navigation.OnboardingRoute
import com.filmax.feature.onboarding.mobile.navigation.onboardingScreen
import com.filmax.feature.player.mobile.navigation.playerScreen
import com.filmax.feature.player.mobile.navigation.trailerScreen
import com.filmax.feature.profile.mobile.navigation.ProfileRoute
import com.filmax.feature.profile.mobile.navigation.deviceSettingsScreen
import com.filmax.feature.profile.mobile.navigation.profileScreen
import com.filmax.feature.search.mobile.navigation.CatalogRoute
import com.filmax.feature.search.mobile.navigation.SearchRoute
import com.filmax.feature.search.mobile.navigation.browseScreen
import com.filmax.feature.search.mobile.navigation.catalogScreen
import com.filmax.feature.search.mobile.navigation.filmographyScreen
import com.filmax.feature.search.mobile.navigation.searchScreen
import com.filmax.feature.tvlink.mobile.navigation.remoteScreen
import com.filmax.feature.tvlink.mobile.navigation.tvPickerDialog
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Serializable
private object SplashRoute

/** Корни разделов: на них — и только на них — видна навигация. */
private val tabRoots = mapOf(
    FilmaxTab.HOME to HomeRoute::class,
    FilmaxTab.CATALOG to CatalogRoute::class,
    FilmaxTab.SEARCH to SearchRoute::class,
    FilmaxTab.MINE to MineRoute::class,
    FilmaxTab.PROFILE to ProfileRoute::class,
)

/**
 * Телефонный граф.
 *
 * Навигация плавает стеклом над корнями пяти разделов и размывает экран под собой; карточка
 * тайтла, подборка, папка, плеер идут на весь экран без неё. Экран не сжимается под навигацию —
 * лента прокручивается под стеклом, а снизу оставляет место [LocalNavBarPadding].
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

    val glass = rememberGlassState()
    // Отступ под навигацию постоянный, а не по видимости бара: иначе на уходе в детали лента
    // раздела дёргалась бы на высоту бара посреди кроссфейда. Читают его только корни разделов.
    val systemBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val navPadding = FilmaxMetrics.NavBarClearance + systemBar
    Box(
        Modifier
            .fillMaxSize()
            .background(FilmaxTheme.palette.background),
    ) {
        CompositionLocalProvider(LocalNavBarPadding provides navPadding) {
            NavHost(
                navController = navController,
                startDestination = SplashRoute,
                modifier = Modifier
                    .fillMaxSize()
                    .glassSource(glass),
                enterTransition = { navFadeIn },
                exitTransition = { navFadeOut },
                popEnterTransition = { navFadeIn },
                popExitTransition = { navFadeOut },
            ) {
                filmaxDestinations(onCheckUpdates)
            }
        }
        FloatingNavigation(
            // Во время ухода бара selectedTab уже null — подсветку на это время держит Главная,
            // иначе на выезде она успевала мигнуть на другой раздел.
            selected = selectedTab,
            onSelect = { navigator.open(it.destination) },
            glass = glass,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * Навигация с затемнением ленты под ней: последние карточки уходят под стекло мягко, а не
 * обрезаются его краем. Бар выезжает и уезжает вместе с кроссфейдом экранов.
 */
@Composable
private fun FloatingNavigation(
    selected: FilmaxTab?,
    onSelect: (FilmaxTab) -> Unit,
    glass: GlassState,
    modifier: Modifier = Modifier,
) {
    val background = FilmaxTheme.palette.background
    AnimatedVisibility(
        visible = selected != null,
        enter = fadeIn(tween(NAV_FADE_MS)) + slideInVertically(tween(NAV_FADE_MS)) { it / 2 },
        exit = fadeOut(tween(NAV_FADE_MS)) + slideOutVertically(tween(NAV_FADE_MS)) { it / 2 },
        modifier = modifier,
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(BottomFadeHeight)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, background.copy(alpha = BOTTOM_FADE_ALPHA)),
                        ),
                    ),
            )
            FilmaxNavBar(
                selected = selected ?: FilmaxTab.HOME,
                onSelect = onSelect,
                glass = glass,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(
                        start = FilmaxMetrics.GridPadding,
                        end = FilmaxMetrics.GridPadding,
                        bottom = FilmaxMetrics.NavBarBottom,
                    ),
            )
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
    catalogScreen()
    searchScreen()
    // Раздел каталога с Главной («Фильмы», «4K и HDR») — тот же Каталог поверх раздела.
    browseScreen()
    collectionDetailScreen()
    feedbackDialog()
    mineScreen()
    folderScreen()
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
    // Телефон как пульт телевизора: выбор ТВ — окном поверх деталей или плеера, пульт — экраном.
    tvPickerDialog()
    remoteScreen()
    designSystemScreen()
}

/** Вкладка навигации — тот же адрес, который называют экраны (Главная → «Каталог» и т.п.). */
private val FilmaxTab.destination: Destination
    get() = when (this) {
        FilmaxTab.HOME -> Destination.Home
        FilmaxTab.CATALOG -> Destination.Catalog
        FilmaxTab.SEARCH -> Destination.Search
        FilmaxTab.MINE -> Destination.Mine
        FilmaxTab.PROFILE -> Destination.Profile
    }

private val BottomFadeHeight = 160.dp
private const val BOTTOM_FADE_ALPHA = 0.8f

package com.filmax.app.tv.navigation

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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalView
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.filmax.app.navigation.RootScreenModel
import com.filmax.app.navigation.navFadeIn
import com.filmax.app.navigation.navFadeOut
import com.filmax.core.tv.designsystem.LocalTvScrollToTop
import com.filmax.feature.collections.common.navigation.CollectionDetailRoute
import com.filmax.feature.collections.tv.navigation.tvCollectionDetailScreen
import com.filmax.feature.details.common.navigation.DetailsRoute
import com.filmax.feature.details.tv.navigation.tvDetailsScreen
import com.filmax.feature.home.tv.navigation.TvHomeRoute
import com.filmax.feature.home.tv.navigation.tvHomeScreen
import com.filmax.feature.library.tv.navigation.tvLibraryScreen
import com.filmax.feature.onboarding.tv.navigation.TvOnboardingRoute
import com.filmax.feature.onboarding.tv.navigation.tvOnboardingScreen
import com.filmax.feature.player.common.navigation.PlayerRoute
import com.filmax.feature.player.common.navigation.TrailerRoute
import com.filmax.feature.player.tv.navigation.tvPlayerScreen
import com.filmax.feature.player.tv.navigation.tvTrailerScreen
import com.filmax.feature.profile.tv.navigation.tvDeviceSettingsScreen
import com.filmax.feature.profile.tv.navigation.tvProfileScreen
import com.filmax.feature.search.common.navigation.FilmographyRoute
import com.filmax.feature.search.tv.navigation.tvFilmographyScreen
import com.filmax.feature.search.tv.navigation.tvSearchScreen
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
    // Попап — до TvHomeRoute, реального корня вкладок: стартовый destination графа — сплэш,
    // которого после логина нет в стеке (popUpTo{inclusive}), и попап по нему молча не
    // срабатывал — каждый заход на вкладку создавал свежую энтри, а restoreState не
    // восстанавливал ни выбранный сегмент, ни скролл.
    fun navigateTab(route: Any) {
        navController.navigate(route) {
            popUpTo<TvHomeRoute> { saveState = true }
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
                enterTransition = { navFadeIn },
                exitTransition = { navFadeOut },
                popEnterTransition = { navFadeIn },
                popExitTransition = { navFadeOut },
            ) {
                tvDestinations(navController)
            }
        }

        if (showTopBar) {
            TvTopNavBar(
                currentDestination = currentDest,
                onSelectTab = { navigateTab(it) },
                focus = TvTopNavBarFocus(navBar = navBarFocus, content = contentFocus),
                initials = rootState.initials,
                // Любой заход фокуса в шапку (с контента или стартовый) — повод увести контент вверх.
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .onFocusChanged { if (it.hasFocus) scrollToTopSignal++ },
            )
        }
    }

    InitialContentFocus(showTopBar = showTopBar, content = contentFocus, navBar = navBarFocus)
}

/**
 * Стартовый фокус — на контенте, а не на шапке: иначе каждый вход в раздел стоит пользователю
 * лишнего «вниз». Контент композится не мгновенно, поэтому сначала пара кадров ожидания.
 *
 * Реквест — только когда фокуса нет нигде (первый вход после сплэша). При возврате из
 * карточки/плеера Compose сам возвращает фокус в контент, и принудительный requestFocus
 * лишь портил картину: фокус улетал на первый focusable экрана (hero), bring-into-view
 * подтягивал к нему прокрутку, и восстановленный скролл терялся.
 * Фоллбек на таб-бар остаётся (гарантия «фокус есть всегда» из гайдлайна Google).
 */
@Composable
private fun InitialContentFocus(showTopBar: Boolean, content: FocusRequester, navBar: FocusRequester) {
    val view = LocalView.current
    LaunchedEffect(showTopBar) {
        if (!showTopBar) return@LaunchedEffect
        // 10 кадров: позже ретраев rememberTvReturnFocus (8), чтобы точечное восстановление
        // фокуса на карточку успело раньше этого фоллбека.
        repeat(10) { withFrameNanos { } }
        if (view.findFocus() != null) return@LaunchedEffect
        runCatching { content.requestFocus() }
            .onFailure { runCatching { navBar.requestFocus() } }
    }
}

/** Регистрация всех экранов TV-графа: сплэш, онбординг, разделы и детали/плеер. */
private fun NavGraphBuilder.tvDestinations(navController: NavHostController) {
    composable<TvSplashRoute> { Box(Modifier.fillMaxSize()) }

    tvOnboardingScreen(
        onAuthenticated = {
            navController.navigate(TvHomeRoute) { popUpTo(TvOnboardingRoute) { inclusive = true } }
        },
    )

    tvHomeScreen(
        onOpenItem = { navController.navigate(DetailsRoute(it)) },
        onPlay = { itemId, season, videoId ->
            navController.navigate(PlayerRoute(itemId, videoId, season))
        },
        onOpenCollection = { id, title ->
            navController.navigate(CollectionDetailRoute(collectionId = id, title = title))
        },
    )
    tvSearchScreen(onOpenItem = { navController.navigate(DetailsRoute(it)) })
    // «Подборки» больше не вкладка — это контент внутри Каталога. Экран содержимого
    // подборки остаётся push-экраном: в него ведёт Каталог.
    tvCollectionDetailScreen(onOpenItem = { navController.navigate(DetailsRoute(it)) })
    // Все карточки «Моё» ведут в карточку тайтла — играть оттуда: кнопка «Продолжить · SxEy».
    tvLibraryScreen(
        onOpenItem = { navController.navigate(DetailsRoute(it)) },
    )
    tvProfileScreen(
        onLogout = {
            navController.navigate(TvOnboardingRoute) { popUpTo(TvHomeRoute) { inclusive = true } }
        },
    )
    // Экран настроек устройства остаётся в графе, но из Профиля временно не открывается:
    // device/info и device/settings на бэкенде отвечают 500.
    tvDeviceSettingsScreen(onBack = { navController.popBackStack() })

    tvDetailsScreen(
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
    tvFilmographyScreen(
        onBack = { navController.popBackStack() },
        onOpenItem = { navController.navigate(DetailsRoute(it)) },
    )
    tvTrailerScreen(onBack = { navController.popBackStack() })
    tvPlayerScreen(
        onBack = { navController.popBackStack() },
        // «Следующая серия» — навигация, а не подмена MediaItem: прогресс пишется в трек,
        // выбранный при старте плеера, и подмена на месте писала бы позицию новой серии
        // в запись предыдущей. popUpTo не копит стек при перещёлкивании серий подряд.
        onPlayEpisode = { itemId, season, videoId ->
            navController.navigate(PlayerRoute(itemId, videoId, season)) {
                popUpTo<PlayerRoute> { inclusive = true }
            }
        },
    )
}

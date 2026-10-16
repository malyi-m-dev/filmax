// Главная: состояние экрана, лента и куда из неё ведут переходы. Ряды ленты — в HomeRows,
// hero — в HomeHero, шапка и баннер «нет сети» — в HomeHeader.

package com.filmax.feature.home.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.domain.feedback.FeedbackTrigger
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.ui.components.FilmaxErrorModal
import com.filmax.feature.home.common.HomeEvent
import com.filmax.feature.home.common.HomeScreenModel
import com.filmax.feature.home.common.HomeSideEffect
import com.filmax.feature.home.common.HomeState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Переходы главной одним типом: пять отдельных лямбд плюс `modifier` и `screenModel` вывели бы
 * сигнатуру приватных composable за порог detekt (LongParameterList = 6). Строится из
 * [Navigator] — граф про эти переходы больше ничего не знает.
 */
internal data class HomeActions(
    val onOpenItem: (Int) -> Unit,
    val onPlay: (itemId: Int, season: Int, videoId: Int) -> Unit,
    val onOpenCollection: (id: Int, title: String) -> Unit,
    val onOpenSearch: () -> Unit,
    val onOpenProfile: () -> Unit,
)

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    screenModel: HomeScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val state by screenModel.collectAsState()
    screenModel.collectSideEffect { effect ->
        when (effect) {
            HomeSideEffect.AskFeedback -> navigator.open(Destination.Feedback(FeedbackTrigger.AfterUpdate.name))
        }
    }
    val appError by screenModel.collectErrorAsState()
    val offline by screenModel.collectOfflineBannerAsState()
    val actions = remember(navigator) { navigator.homeActions() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxSize()) {
            // Шапка вне ленты: она не скроллится, поэтому и не лежит в LazyColumn.
            HomeTopBar(
                initials = state.initials,
                onOpenSearch = actions.onOpenSearch,
                onOpenProfile = actions.onOpenProfile,
            )
            if (state.loading) {
                Box(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                HomeFeed(
                    state = state,
                    offline = offline,
                    actions = actions,
                    onEvent = screenModel::dispatch,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        appError?.let { error ->
            FilmaxErrorModal(
                error = error,
                onDismiss = screenModel::dismissError,
                onPrimary = screenModel::retry,
            )
        }
    }
}

@Composable
private fun HomeFeed(
    state: HomeState,
    offline: Boolean,
    actions: HomeActions,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        // Лента кончается тем же полем, что и сетки Каталога и «Моё»: без него ряд «Подборки»
        // упирается прямо в таб-бар.
        contentPadding = PaddingValues(bottom = FilmaxMetrics.ListBottomPadding),
    ) {
        // Офлайн-деградация (issue #42): контент из кэша + ненавязчивый баннер вместо модалки.
        if (offline) {
            item(key = "offline") { OfflineBanner(onReload = { onEvent(HomeEvent.Load) }) }
        }
        state.hero?.let { hero ->
            item(key = "hero") {
                HomeHero(
                    item = hero,
                    // Фильм — единственный трек, эпизод выбирать не из чего.
                    onPlay = { actions.onPlay(hero.id, NO_SEASON, NO_VIDEO_ID) },
                    onOpenItem = { actions.onOpenItem(hero.id) },
                )
            }
        }
        homeRows(state = state, actions = actions, onEvent = onEvent)
    }
}

/**
 * Куда ведёт главная. Ряд «Продолжить» и кнопка hero открывают плеер напрямую, минуя детали:
 * недосмотренный эпизод — это сезон и номер видео из истории.
 */
private fun Navigator.homeActions() = HomeActions(
    onOpenItem = { itemId -> open(Destination.Details(itemId)) },
    onPlay = { itemId, season, videoId ->
        open(Destination.Player(itemId = itemId, videoId = videoId, season = season))
    },
    onOpenCollection = { id, title -> open(Destination.CollectionDetail(id, title)) },
    onOpenSearch = { open(Destination.Catalog) },
    onOpenProfile = { open(Destination.Profile) },
)

/** `PlayerRoute.videoId` для фильма/неизвестного эпизода — плеер возьмёт первый трек. */
internal const val NO_VIDEO_ID = -1

/** `PlayerRoute.season` для фильма/неизвестного сезона. */
internal const val NO_SEASON = -1

// Главная: состояние экрана, лента и куда из неё ведут переходы. Ряды ленты — в HomeRows,
// hero — в HomeHero, шапка, чипы и баннер «нет сети» — в HomeHeader, шторка разделов — в HomeSections.

package com.filmax.feature.home.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.glassSource
import com.filmax.core.designsystem.rememberGlassState
import com.filmax.core.domain.catalog.CatalogSection
import com.filmax.core.domain.feedback.FeedbackTrigger
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.ui.components.FilmaxErrorModal
import com.filmax.core.ui.components.LocalNavBarPadding
import com.filmax.feature.home.common.HomeEvent
import com.filmax.feature.home.common.HomeScreenModel
import com.filmax.feature.home.common.HomeSideEffect
import com.filmax.feature.home.common.HomeState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Переходы и события главной одним типом: отдельные лямбды плюс `modifier` и `screenModel` вывели бы
 * сигнатуру приватных composable за порог detekt (LongParameterList = 6). Строится из
 * [Navigator] — граф про эти переходы больше ничего не знает.
 */
internal data class HomeActions(
    val onOpenItem: (Int) -> Unit,
    val onPlay: (itemId: Int, season: Int, videoId: Int) -> Unit,
    val onOpenCollection: (id: Int, title: String) -> Unit,
    val onOpenSection: (CatalogSection) -> Unit,
    val onOpenTv: () -> Unit,
    /** События ленты: догрузка рядов, «Буду смотреть», повтор загрузки. */
    val onEvent: (HomeEvent) -> Unit,
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
    val actions = remember(navigator, screenModel) { navigator.homeActions(onEvent = screenModel::dispatch) }
    var sectionsOpen by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FilmaxTheme.palette.background),
    ) {
        if (state.loading) {
            CircularProgressIndicator(color = FilmaxTheme.palette.accent, modifier = Modifier.align(Alignment.Center))
        } else {
            HomeFeedWithHeader(
                state = state,
                offline = offline,
                actions = actions,
                onOpenSections = { sectionsOpen = true },
            )
        }
        if (sectionsOpen) {
            HomeSectionsSheet(onOpenSection = actions.onOpenSection, onDismiss = { sectionsOpen = false })
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

/**
 * Лента и шапка над ней. Шапка лежит поверх ленты: над hero она прозрачная, а когда hero уезжает
 * под неё, проявляется стекло — лента размывается под шапкой, а не обрезается её краем.
 */
@Composable
private fun HomeFeedWithHeader(
    state: HomeState,
    offline: Boolean,
    actions: HomeActions,
    onOpenSections: () -> Unit,
) {
    val glass = rememberGlassState()
    val listState = rememberLazyListState()
    val heroHeight = rememberHeroHeight()
    val heroHeightPx = with(LocalDensity.current) { heroHeight.toPx() }
    val glassProgress by remember(listState, heroHeightPx) {
        derivedStateOf { listState.scrolledPast(heroHeightPx * GLASS_FROM, heroHeightPx * GLASS_TO) }
    }
    Box(Modifier.fillMaxSize()) {
        HomeFeed(
            state = state,
            offline = offline,
            actions = actions,
            layout = FeedLayout(listState, heroHeight),
            modifier = Modifier.glassSource(glass),
        )
        HomeHeader(
            glass = glass,
            glassProgress = { glassProgress },
            onOpenTv = actions.onOpenTv,
            onOpenSection = actions.onOpenSection,
            onOpenSections = onOpenSections,
        )
    }
}

/** Прокрутка ленты и высота hero — от них зависит, когда проявляется стекло шапки. */
private data class FeedLayout(val listState: LazyListState, val heroHeight: Dp)

@Composable
private fun HomeFeed(
    state: HomeState,
    offline: Boolean,
    actions: HomeActions,
    layout: FeedLayout,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = layout.listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = LocalNavBarPadding.current),
    ) {
        if (state.heroes.isNotEmpty()) {
            item(key = "hero") {
                val heroActions = remember(actions) {
                    HeroActions(
                        // Без серии и сезона: плеер возьмёт первый трек или продолжит с сохранённого.
                        onPlay = { hero -> actions.onPlay(hero.id, NO_SEASON, NO_VIDEO_ID) },
                        onOpenItem = { hero -> actions.onOpenItem(hero.id) },
                        onToggleWatchLater = { hero -> actions.onEvent(HomeEvent.ToggleWatchLater(hero)) },
                    )
                }
                HomeHero(
                    heroes = state.heroes,
                    watchLaterIds = state.watchLaterIds,
                    height = layout.heroHeight,
                    actions = heroActions,
                )
            }
        }
        // Офлайн-деградация (issue #42): контент из кэша + ненавязчивый баннер вместо модалки.
        if (offline) {
            item(key = "offline") { OfflineBanner(onReload = { actions.onEvent(HomeEvent.Load) }) }
        }
        homeRows(state = state, actions = actions)
    }
}

/**
 * Насколько лента ушла под шапку: 0 — hero ещё на месте, 1 — уехал за [to] пикселей. Дальше
 * первого элемента — всегда 1: hero уже целиком за краем.
 */
private fun LazyListState.scrolledPast(from: Float, to: Float): Float = when {
    firstVisibleItemIndex > 0 -> 1f
    else -> ((firstVisibleItemScrollOffset - from) / (to - from)).coerceIn(0f, 1f)
}

/**
 * Куда ведёт главная. Ряд «Продолжить» и кнопка hero открывают плеер напрямую, минуя детали:
 * недосмотренный эпизод — это сезон и номер видео из истории.
 */
private fun Navigator.homeActions(onEvent: (HomeEvent) -> Unit) = HomeActions(
    onOpenItem = { itemId -> open(Destination.Details(itemId)) },
    onPlay = { itemId, season, videoId ->
        open(Destination.Player(itemId = itemId, videoId = videoId, season = season))
    },
    onOpenCollection = { id, title -> open(Destination.CollectionDetail(id, title)) },
    onOpenSection = { section -> open(Destination.Browse(section.name)) },
    // Без тайтла: телевизор выбирают ради пульта.
    onOpenTv = { open(Destination.TvPicker()) },
    onEvent = onEvent,
)

/** Стекло шапки проявляется, пока hero уезжает с 55% до 85% своей высоты. */
private const val GLASS_FROM = 0.55f
private const val GLASS_TO = 0.85f

/** `PlayerRoute.videoId` для фильма/неизвестного эпизода — плеер возьмёт первый трек. */
internal const val NO_VIDEO_ID = -1

/** `PlayerRoute.season` для фильма/неизвестного сезона. */
internal const val NO_SEASON = -1

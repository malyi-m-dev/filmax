// Главная: состояние экрана и лента. Ряды ленты — в HomeRows, hero — в HomeHero, шапка, чипы и
// баннер «нет сети» — в HomeHeader, шторка разделов — в HomeSections. Куда ведут карточки и
// кнопки, решает HomeScreenModel: экран только шлёт события.

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.glassSource
import com.filmax.core.designsystem.rememberGlassState
import com.filmax.core.ui.components.FilmaxErrorModal
import com.filmax.core.ui.components.LocalNavBarPadding
import com.filmax.feature.home.common.HomeEvent
import com.filmax.feature.home.common.HomeScreenModel
import com.filmax.feature.home.common.HomeState
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    screenModel: HomeScreenModel = koinViewModel(),
) {
    val state by screenModel.collectAsState()
    val appError by screenModel.collectErrorAsState()
    val offline by screenModel.collectOfflineBannerAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FilmaxTheme.palette.background),
    ) {
        if (state.loading) {
            CircularProgressIndicator(color = FilmaxTheme.palette.accent, modifier = Modifier.align(Alignment.Center))
        } else {
            HomeFeedWithHeader(state = state, offline = offline, onEvent = screenModel::dispatch)
        }
        if (state.sectionsOpen) {
            HomeSectionsSheet(onEvent = screenModel::dispatch)
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
private fun HomeFeedWithHeader(state: HomeState, offline: Boolean, onEvent: (HomeEvent) -> Unit) {
    val glass = rememberGlassState()
    val listState = rememberLazyListState()
    val heroHeightPx = with(LocalDensity.current) { rememberHeroHeight().toPx() }
    val glassProgress by remember(listState, heroHeightPx) {
        derivedStateOf { listState.scrolledPast(heroHeightPx * GLASS_FROM, heroHeightPx * GLASS_TO) }
    }
    Box(Modifier.fillMaxSize()) {
        HomeFeed(
            state = state,
            offline = offline,
            onEvent = onEvent,
            listState = listState,
            modifier = Modifier.glassSource(glass),
        )
        HomeHeader(glass = glass, glassProgress = { glassProgress }, onEvent = onEvent)
    }
}

@Composable
private fun HomeFeed(
    state: HomeState,
    offline: Boolean,
    onEvent: (HomeEvent) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val heroHeight = rememberHeroHeight()
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = LocalNavBarPadding.current),
    ) {
        if (state.heroes.isNotEmpty()) {
            item(key = "hero") {
                HomeHero(
                    heroes = state.heroes,
                    watchLaterIds = state.watchLaterIds,
                    height = heroHeight,
                    onEvent = onEvent,
                )
            }
        }
        // Офлайн-деградация (issue #42): контент из кэша + ненавязчивый баннер вместо модалки.
        if (offline) {
            item(key = "offline") { OfflineBanner(onReload = { onEvent(HomeEvent.Load) }) }
        }
        homeRows(state = state, onEvent = onEvent)
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

/** Стекло шапки проявляется, пока hero уезжает с 55% до 85% своей высоты. */
private const val GLASS_FROM = 0.55f
private const val GLASS_TO = 0.85f

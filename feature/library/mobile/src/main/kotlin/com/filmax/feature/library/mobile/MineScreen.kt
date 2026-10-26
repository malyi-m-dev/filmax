// «Моё»: стеклянная шапка с сегментами и сегменты страницами, которые листаются пальцем. Сами
// сегменты — в MineSegments, шторки создания папки и подтверждений — в MineSheets. Куда ведут
// карточки и что подтверждать, решает MineScreenModel: экран шлёт события.

package com.filmax.feature.library.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.GlassState
import com.filmax.core.designsystem.glassSource
import com.filmax.core.designsystem.rememberGlassState
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.ui.components.FilmaxChip
import com.filmax.core.ui.components.FilmaxGlassHeader
import com.filmax.core.ui.components.FilmaxIconButton
import com.filmax.core.ui.components.LocalNavBarPadding
import com.filmax.feature.library.common.MineEvent
import com.filmax.feature.library.common.MineScreenModel
import com.filmax.feature.library.common.MineSegment
import com.filmax.feature.library.common.MineSideEffect
import com.filmax.feature.library.common.MineState
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun MineScreen(
    modifier: Modifier = Modifier,
    screenModel: MineScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val state by screenModel.collectAsState()
    screenModel.collectSideEffect { effect ->
        when (effect) {
            is MineSideEffect.OpenDetails -> navigator.open(Destination.Details(effect.itemId))
            is MineSideEffect.OpenFolder -> navigator.open(Destination.BookmarkFolder(effect.folderId))
            MineSideEffect.OpenSearch -> navigator.open(Destination.Search)
            is MineSideEffect.OpenPlayer ->
                navigator.open(Destination.Player(effect.itemId, effect.videoId, effect.season))
        }
    }
    // Вернулись из папки, деталей или плеера — история и папки могли измениться.
    LifecycleResumeEffect(Unit) {
        screenModel.dispatch(MineEvent.Refresh)
        onPauseOrDispose { }
    }
    Box(modifier.fillMaxSize().background(FilmaxTheme.palette.background)) {
        MineBody(state = state, onEvent = screenModel::dispatch)
    }
    MineSheets(state = state, onEvent = screenModel::dispatch)
}

@Composable
private fun MineBody(state: MineState, onEvent: (MineEvent) -> Unit) {
    val glass = rememberGlassState()
    val density = LocalDensity.current
    var headerHeight by remember { mutableStateOf(0.dp) }
    val pager = rememberPagerState(initialPage = state.segment.ordinal) { MineSegment.entries.size }
    // Сегмент — состояние модели: пролистали пальцем — модель узнаёт, какой сегмент на экране.
    LaunchedEffect(pager.settledPage) {
        val shown = MineSegment.entries[pager.settledPage]
        if (shown != state.segment) onEvent(MineEvent.SelectSegment(shown))
    }
    val padding = PaddingValues(
        start = FilmaxMetrics.GridPadding,
        end = FilmaxMetrics.GridPadding,
        top = headerHeight + 12.dp,
        bottom = LocalNavBarPadding.current + FilmaxMetrics.GridPadding,
    )
    Box(Modifier.fillMaxSize()) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize().glassSource(glass)) { page ->
            MineSegmentPage(
                segment = MineSegment.entries[page],
                state = state,
                onEvent = onEvent,
                contentPadding = padding
            )
        }
        MineHeader(
            pager = pager,
            glass = glass,
            onEvent = onEvent,
            modifier = Modifier.onSizeChanged { headerHeight = with(density) { it.height.toDp() } },
        )
    }
}

@Composable
private fun MineHeader(
    pager: PagerState,
    glass: GlassState,
    onEvent: (MineEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    FilmaxGlassHeader(glass = glass, modifier = modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(FilmaxMetrics.HeaderRowHeight)
                .padding(start = FilmaxMetrics.ScreenPadding, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Моё", style = FilmaxType.screenTitle, color = FilmaxColors.Text, modifier = Modifier.weight(1f))
            FilmaxIconButton(
                Icons.Filled.Search,
                contentDescription = "Поиск",
                onClick = { onEvent(MineEvent.OpenSearch) },
                size = FilmaxMetrics.IconButtonSmall,
            )
        }
        // Сегмент пролистали пальцем — его чип выезжает в ряд: «История» на узком телефоне за краем.
        val chips = rememberLazyListState()
        LaunchedEffect(pager.currentPage) { chips.animateScrollToItem(pager.currentPage) }
        LazyRow(
            state = chips,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 14.dp),
            contentPadding = PaddingValues(horizontal = FilmaxMetrics.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(MineSegment.entries, key = { it.name }) { segment ->
                FilmaxChip(
                    label = segment.label,
                    selected = pager.currentPage == segment.ordinal,
                    onClick = { scope.launch { pager.animateScrollToPage(segment.ordinal) } },
                )
            }
        }
    }
}

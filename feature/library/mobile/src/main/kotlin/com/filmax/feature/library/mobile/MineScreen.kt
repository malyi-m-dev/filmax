// «Моё»: стеклянная шапка с сегментами и сегменты страницами, которые листаются пальцем. Сами
// сегменты — в MineSegments, шторки создания папки и подтверждений — в MineSheets. Состояние
// экрана целиком в MineScreenModel; в вёрстке — только прокрутка и замер шапки.

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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.GlassState
import com.filmax.core.designsystem.glassSource
import com.filmax.core.designsystem.rememberGlassState
import com.filmax.core.domain.catalog.model.VideoRef
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.Navigator
import com.filmax.core.ui.components.FilmaxChip
import com.filmax.core.ui.components.FilmaxGlassHeader
import com.filmax.core.ui.components.FilmaxIconButton
import com.filmax.core.ui.components.LocalNavBarPadding
import com.filmax.core.ui.preview.PhonePreviews
import com.filmax.feature.library.common.MineEvent
import com.filmax.feature.library.common.MineSamples
import com.filmax.feature.library.common.MineScreenModel
import com.filmax.feature.library.common.MineSegment
import com.filmax.feature.library.common.MineState
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** Переходы и события «Моё» одним объектом: порог LongParameterList у detekt — 6. */
internal data class MineActions(
    val onEvent: (MineEvent) -> Unit,
    val onOpenItem: (Int) -> Unit,
    val onPlay: (itemId: Int, video: VideoRef?) -> Unit,
    val onOpenFolder: (Int) -> Unit,
    val onOpenSearch: () -> Unit,
) {
    companion object {
        /** Ничего не делает — для превью. */
        val None = MineActions(
            onEvent = {},
            onOpenItem = {},
            onPlay = { _, _ -> },
            onOpenFolder = {},
            onOpenSearch = {},
        )
    }
}

/** «Моё» на телефоне: модель, навигатор и перечитывание при возврате на экран. */
@Composable
fun MineScreen(
    modifier: Modifier = Modifier,
    screenModel: MineScreenModel = koinViewModel(),
    navigator: Navigator = koinInject(),
) {
    val state by screenModel.collectAsState()
    // Вернулись из папки, деталей или плеера — история и папки могли измениться.
    LifecycleResumeEffect(Unit) {
        screenModel.dispatch(MineEvent.Refresh)
        onPauseOrDispose { }
    }
    val actions = remember(screenModel, navigator) { mineActions(screenModel::dispatch, navigator) }
    MineScreen(state = state, actions = actions, modifier = modifier)
}

/** «Моё» из состояния — без Koin и навигатора: его рисуют превью и UI-тесты. */
@Composable
internal fun MineScreen(state: MineState, actions: MineActions, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(FilmaxTheme.palette.background)) {
        MinePages(state = state, actions = actions)
    }
    MineSheets(state = state, onEvent = actions.onEvent)
}

private fun mineActions(onEvent: (MineEvent) -> Unit, navigator: Navigator) = MineActions(
    onEvent = onEvent,
    onOpenItem = { navigator.open(Destination.Details(it)) },
    onPlay = { itemId, video -> navigator.open(Destination.Player(itemId = itemId, video = video)) },
    onOpenFolder = { navigator.open(Destination.BookmarkFolder(it)) },
    onOpenSearch = { navigator.open(Destination.Search) },
)

/** Шапка поверх страниц: её высоту замеряем, чтобы лента начиналась под стеклом. */
@Composable
private fun MinePages(state: MineState, actions: MineActions) {
    val glass = rememberGlassState()
    val density = LocalDensity.current
    var headerHeight by remember { mutableStateOf(0.dp) }
    val segments = state.segments
    val pager = rememberPagerState(initialPage = segments.indexOf(state.segment).coerceAtLeast(0)) { segments.size }
    // Сегмент — состояние модели: пролистали пальцем — модель узнаёт, какой сегмент на экране.
    LaunchedEffect(pager.settledPage) {
        val shown = segments[pager.settledPage]
        if (shown != state.segment) actions.onEvent(MineEvent.SelectSegment(shown))
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
                segment = segments[page],
                state = state,
                actions = actions,
                contentPadding = padding
            )
        }
        MineHeader(
            segments = segments,
            pager = pager,
            glass = glass,
            onOpenSearch = actions.onOpenSearch,
            modifier = Modifier.onSizeChanged { headerHeight = with(density) { it.height.toDp() } },
        )
    }
}

@Composable
private fun MineHeader(
    segments: List<MineSegment>,
    pager: PagerState,
    glass: GlassState,
    onOpenSearch: () -> Unit,
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
                onClick = onOpenSearch,
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
            itemsIndexed(segments, key = { _, segment -> segment.name }) { index, segment ->
                FilmaxChip(
                    label = segment.label,
                    selected = pager.currentPage == index,
                    onClick = { scope.launch { pager.animateScrollToPage(index) } },
                )
            }
        }
    }
}

private class MineStatePreviews : PreviewParameterProvider<MineState> {
    override val values = MineSamples.all.asSequence()
}

@PhonePreviews
@Composable
private fun MineScreenPreview(@PreviewParameter(MineStatePreviews::class) state: MineState) {
    FilmaxTheme { MineScreen(state = state, actions = MineActions.None) }
}

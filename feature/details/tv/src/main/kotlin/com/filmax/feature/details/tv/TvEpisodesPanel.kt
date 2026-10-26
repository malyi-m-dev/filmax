// Панель «Эпизоды» в деталях сериала: сезоны чипами и серии списком. Прокручивается только она.
// Что в ней показано (строки, чипы, соседние сезоны), считает модель — DetailsState.episodes.
@file:OptIn(ExperimentalFoundationApi::class)

package com.filmax.feature.details.tv

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.filmax.core.tv.designsystem.FxMotion
import com.filmax.core.tv.designsystem.FxType
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.LocalReducedMotion
import com.filmax.core.tv.designsystem.PivotBringIntoViewSpec
import com.filmax.core.tv.designsystem.TvChip
import com.filmax.core.tv.designsystem.TvChipStyle
import com.filmax.core.tv.designsystem.TvEpisodeRow
import com.filmax.core.tv.designsystem.TvEpisodeRowModel
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvScreenFocus
import com.filmax.core.tv.designsystem.rememberTvScrollFades
import com.filmax.core.tv.designsystem.tvFocusGroup
import com.filmax.core.tv.designsystem.tvScrollFades
import com.filmax.feature.details.common.DetailsEvent
import com.filmax.feature.details.common.EpisodeRow
import com.filmax.feature.details.common.EpisodesPanel
import com.filmax.feature.details.common.SeasonStart

/** Серия в фокусе держится третьей сверху: видно и что было, и что дальше. */
private val EpisodePivot = 162.dp

/** Панель отстоит от краёв экрана справа, сверху и снизу. */
private val PanelMargin = 24.dp
private val PanelPaddingTop = 18.dp
private val PanelPadding = 16.dp

/** Заголовок панели чуть глубже чипов и строк — в линию с текстом строк, а не с их кадрами. */
private val HeaderInset = 4.dp

/**
 * Тёмное стекло: фон экрана на 72% и тонкая светлая обводка. Светлое стекло на ярком кадре
 * сериала теряло текст серий — тёмное держит контраст на любом фоне.
 */
private const val GLASS_ALPHA = 0.72f
private const val GLASS_OUTLINE_ALPHA = 0.1f

/** Последняя серия проскролливается выше нижнего затухания. */
private val ListBottomPadding = 40.dp

/** Сколько серий выше стартовой оставить видимыми при открытии сезона. */
private const val ROWS_ABOVE_START = 2

/** Смена сезона: новые серии поднимаются на 12dp и проявляются, старые гаснут быстрее. */
private val SeasonSlide = 12.dp
private const val SEASON_EXIT_MS = 120

/**
 * Стеклянная панель «Эпизоды»: шапка с числом серий, сезоны компактными чипами и серии
 * списком. Сезон выбирается фокусом на чипе — модель выжидает 250 мс, прежде чем его сменить, —
 * или стрелками «←/→» прямо с серии, без подъёма к чипам.
 * [exitLeft] — куда уводит «←» из панели (главная кнопка, а не описание) с первого сезона: с
 * остальных «←» листает сезоны назад. Задаётся на каждой строке серий и на первом чипе сезонов:
 * направленные свойства Compose берёт у узла под фокусом, а не у контейнера.
 */
@Composable
internal fun TvEpisodesPanel(
    panel: EpisodesPanel,
    onEvent: (DetailsEvent) -> Unit,
    focus: TvScreenFocus,
    exitLeft: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val glass = LocalFxColors.current.background.copy(alpha = GLASS_ALPHA)
    Column(
        modifier
            .width(TvMetrics.EpisodesPanelWidth + PanelMargin)
            .padding(top = PanelMargin, end = PanelMargin, bottom = PanelMargin)
            .background(glass, TvMetrics.SheetShape)
            .border(1.dp, Color.White.copy(alpha = GLASS_OUTLINE_ALPHA), TvMetrics.SheetShape)
            .padding(top = PanelPaddingTop),
    ) {
        val exit = Modifier.focusProperties { left = exitLeft }
        PanelHeader(panel.summary)
        SeasonChips(panel, onEvent, exit)
        SeasonEpisodes(panel, onEvent, focus, exit, Modifier.weight(1f))
    }
}

@Composable
private fun PanelHeader(summary: String?) {
    val colors = LocalFxColors.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = PanelPadding + HeaderInset),
        verticalAlignment = Alignment.Bottom
    ) {
        Text("Эпизоды", style = FxType.rowTitle, color = colors.text, modifier = Modifier.weight(1f))
        summary?.let { Text(it, style = FxType.metaOnImage, color = colors.textDim) }
    }
}

@Composable
private fun SeasonChips(panel: EpisodesPanel, onEvent: (DetailsEvent) -> Unit, exit: Modifier) {
    val seasons = panel.seasons
    if (seasons.size < 2) return
    val selectedChip = remember { FocusRequester() }
    val selectedIndex = seasons.indexOfFirst { it.selected }
    val rowState = rememberLazyListState()
    KeepChipInView(rowState, selectedIndex)
    val firstSeason = seasons.first().number
    LazyRow(
        state = rowState,
        modifier = Modifier.enterOnSelected(rowState, selectedIndex, selectedChip),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = PanelPadding),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(seasons, key = { it.number }) { season ->
            TvChip(
                label = if (season.selected) season.label else season.compactLabel,
                selected = season.selected,
                onClick = { onEvent(DetailsEvent.SelectSeason(season.number)) },
                style = TvChipStyle.Compact,
                // Сезон меняется фокусом, без «ОК»: модель гасит быстрое пролистывание задержкой.
                // «←» уводит на кнопки только с первого чипа — с остальных листает сезоны назад.
                modifier = Modifier
                    .then(if (season.number == firstSeason) exit else Modifier)
                    .then(if (season.selected) Modifier.focusRequester(selectedChip) else Modifier)
                    .onFocusChanged { if (it.isFocused) onEvent(DetailsEvent.SelectSeason(season.number)) },
            )
        }
    }
}

/**
 * Вход в ряд из серий — на выбранный сезон: ближайший чип переключил бы сезон фокусом. Не
 * `tvFocusGroup(fallback)`: тот просит фокус у fallback вслепую, а у длинного сериала выбранный
 * чип бывает за краем ряда, не размещён — и Compose 1.8 на таком запросе падает. Пока ряд
 * докручивает до выбранного чипа ([KeepChipInView]), вход отменяется: фокус остаётся на серии.
 */
private fun Modifier.enterOnSelected(rowState: LazyListState, index: Int, selectedChip: FocusRequester): Modifier =
    focusProperties {
        onEnter = { if (rowState.isPlaced(index)) selectedChip.requestFocus() else cancelFocusChange() }
    }.focusGroup()

/**
 * Выбранный чип — целиком в видимой части ряда. Стрелками с серий сезон листается мимо ряда, и у
 * длинного сериала выбранный чип уезжал бы за край: не видно, какой сезон открыт, и войти в ряд на
 * него нельзя. То же при открытии сериала на дальнем сезоне. Прокрутка оставляет слева соседний
 * чип — видно, что ряд продолжается.
 */
@Composable
private fun KeepChipInView(rowState: LazyListState, index: Int) {
    LaunchedEffect(index) {
        if (index >= 0 && !rowState.isFullyVisible(index)) rowState.animateScrollToItem((index - 1).coerceAtLeast(0))
    }
}

private fun LazyListState.isPlaced(index: Int): Boolean = layoutInfo.visibleItemsInfo.any { it.index == index }

private fun LazyListState.isFullyVisible(index: Int): Boolean {
    val layout = layoutInfo
    val item = layout.visibleItemsInfo.firstOrNull { it.index == index } ?: return false
    return item.offset >= layout.viewportStartOffset && item.offset + item.size <= layout.viewportEndOffset
}

/**
 * Серии выбранного сезона. Сезон сменился — старый список гаснет, новый поднимается и
 * открывается на серии «продолжить» или первой недосмотренной. Сменили чипом — фокус в это
 * время на чипе, и смена списка его не уносит; стрелкой с серии — фокус переходит на серию,
 * с которой открылся новый сезон ([seasonArrows]).
 */
@Composable
private fun SeasonEpisodes(
    panel: EpisodesPanel,
    onEvent: (DetailsEvent) -> Unit,
    focus: TvScreenFocus,
    exit: Modifier,
    modifier: Modifier,
) {
    val slide = with(LocalDensity.current) { SeasonSlide.roundToPx() }
    val reduced = LocalReducedMotion.current
    val selected = panel.seasons.firstOrNull { it.selected }?.number
    // Состояние несёт строки только выбранного сезона, а уходящий список должен догореть со
    // своими: пустой он отпустил бы фокус раньше, чем разместится серия нового сезона, и фокус
    // упал бы на чип — а это уже выбор сезона чипом.
    val shownPanels = remember { mutableMapOf<Int?, EpisodesPanel>() }
    shownPanels[selected] = panel
    AnimatedContent(
        targetState = selected,
        transitionSpec = { seasonTransition(slide, reduced) },
        modifier = modifier.seasonArrows(panel, onEvent, focus),
        label = "season",
    ) { season ->
        EpisodeList(shownPanels.getValue(season), onEvent, focus, exit)
    }
}

/**
 * «←/→» с серии листает сезоны. Строка в фокусе уходит вместе со старым списком, поэтому фокус
 * заранее назначен серии, с которой откроется новый сезон: она примет его, как только
 * разместится. Листать некуда — стрелка не перехвачена и работает как обычно: «←» с первого
 * сезона уводит на кнопки.
 */
private fun Modifier.seasonArrows(
    panel: EpisodesPanel,
    onEvent: (DetailsEvent) -> Unit,
    focus: TvScreenFocus,
): Modifier =
    onKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
        val target: SeasonStart = when (event.key) {
            Key.DirectionLeft -> panel.previous
            Key.DirectionRight -> panel.next
            else -> null
        } ?: return@onKeyEvent false
        focus.focusOn(episodeKey(target.episodeId))
        onEvent(DetailsEvent.SwitchSeason(target.season))
        true
    }

private fun AnimatedContentTransitionScope<Int?>.seasonTransition(slide: Int, reduced: Boolean): ContentTransform =
    if (reduced) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        (fadeIn(FxMotion.fade()) + slideInVertically(FxMotion.fade()) { slide }) togetherWith
            fadeOut(tween(SEASON_EXIT_MS))
    }

/**
 * Список серий сезона, открытый на серии «продолжить» или первой недосмотренной. Края списка
 * затухают там, где за ними есть ещё серии ([tvScrollFades]); последняя серия проскролливается
 * выше нижнего затухания.
 */
@Composable
private fun EpisodeList(panel: EpisodesPanel, onEvent: (DetailsEvent) -> Unit, focus: TvScreenFocus, exit: Modifier) {
    val pivot = with(LocalDensity.current) { EpisodePivot.toPx() }
    val spec = remember(pivot) { PivotBringIntoViewSpec(pivot) }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (panel.startIndex - ROWS_ABOVE_START).coerceAtLeast(0),
    )
    val fades = rememberTvScrollFades(listState)
    CompositionLocalProvider(LocalBringIntoViewSpec provides spec) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().tvScrollFades(fades).tvFocusGroup(),
            contentPadding = PaddingValues(
                start = PanelPadding,
                end = PanelPadding,
                top = 2.dp,
                bottom = ListBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(panel.rows, key = { it.episode.id }) { row ->
                val episode = row.episode
                TvEpisodeRow(
                    model = row.toTvModel(),
                    // Плееру нужны номер серии (API `video`) и сезон, а не id трека.
                    onClick = { onEvent(DetailsEvent.PlayEpisode(episode.seasonNumber, episode.number)) },
                    modifier = focus.item(episodeKey(episode.id)).then(exit),
                )
            }
        }
    }
}

/** Строка серии из State — в модель строки дизайн-системы: те же поля, другой модуль. */
private fun EpisodeRow.toTvModel() = TvEpisodeRowModel(
    title = title,
    meta = meta,
    imageUrl = episode.thumbnail,
    progress = progress,
    watched = watched,
    resume = resume,
)

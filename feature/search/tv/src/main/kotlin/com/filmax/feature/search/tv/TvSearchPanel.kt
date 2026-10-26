// Режим поиска (макет S06): экранная клавиатура и недавние запросы слева, «Лучшее совпадение»
// крупной карточкой и остальные результаты постерами справа.
package com.filmax.feature.search.tv

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.presentation.labels.isHighRating
import com.filmax.core.presentation.labels.posterMeta
import com.filmax.core.presentation.labels.ratingLabel
import com.filmax.core.presentation.labels.typeLabel
import com.filmax.core.tv.designsystem.FxMotion
import com.filmax.core.tv.designsystem.FxType
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.LocalReducedMotion
import com.filmax.core.tv.designsystem.TvKeyboard
import com.filmax.core.tv.designsystem.TvKeyboardActions
import com.filmax.core.tv.designsystem.TvKeyboardLayouts
import com.filmax.core.tv.designsystem.TvKeyboardWidth
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOverline
import com.filmax.core.tv.designsystem.TvPosterCard
import com.filmax.core.tv.designsystem.TvPosterDetails
import com.filmax.core.tv.designsystem.TvPosterSize
import com.filmax.core.tv.designsystem.TvSpotlight
import com.filmax.core.tv.designsystem.TvSpotlightCard
import com.filmax.core.tv.designsystem.TvTextRow
import com.filmax.core.tv.designsystem.TvTextRowInset
import com.filmax.core.tv.designsystem.fxEnter
import com.filmax.core.tv.designsystem.tvFocusGroup
import com.filmax.feature.search.common.KeyboardLayoutLabels
import com.filmax.feature.search.common.SPACE_KEY
import com.filmax.feature.search.common.SearchEvent
import com.filmax.feature.search.common.SearchState
import com.filmax.feature.search.common.keyboardRows
import com.filmax.feature.search.common.matchRange
import com.filmax.feature.search.common.moreResultsLabel
import com.filmax.feature.search.common.searchHint

/** Результаты справа — по четыре постера 106dp с шагом 16dp на 470dp колонки. */
private const val RESULT_COLUMNS = 4
private val ResultGap = 16.dp

/** По вертикали — 12dp от надзаголовка до карточек; между лучшим совпадением и «Ещё» — 22dp. */
private val ResultRowGap = 12.dp
private val MoreResultsTop = 10.dp

/** Отступы S06: строка поиска → колонки 26dp, клавиатура → результаты 40dp. */
private val BarToColumns = 26.dp
private val ColumnsGap = 40.dp
private val KeyboardToLayouts = 16.dp
private val LayoutsToRecent = 22.dp

/** Надзаголовок «Недавние» → первая строка: 10dp до текста, из них 3dp — поле плотной строки. */
private val RecentTitleGap = 7.dp

@Composable
internal fun TvSearchPanel(
    state: SearchState,
    onEvent: (SearchEvent) -> Unit,
    onVoice: () -> Unit,
    focus: CatalogScreenFocus,
) {
    val bestMatch = remember { FocusRequester() }
    Column(
        Modifier
            .fillMaxSize()
            .padding(start = TvMetrics.SectionStart, end = TvMetrics.SectionEnd, top = TvMetrics.ContentTop),
    ) {
        CatalogSearchBar(state = state, onEvent = onEvent, onVoice = onVoice, focus = focus)
        Row(Modifier.padding(top = BarToColumns)) {
            KeyboardColumn(
                state = state,
                onEvent = onEvent,
                focus = focus,
                // «→» с правого края клавиатуры — на лучшее совпадение, а не на ближайший постер.
                exitRight = bestMatch.takeIf { state.bestMatch != null },
            )
            SearchResults(
                state = state,
                onEvent = onEvent,
                focus = focus,
                bestMatch = bestMatch,
                modifier = Modifier.padding(start = ColumnsGap).weight(1f),
            )
        }
    }
}

/** Левая колонка: клавиатура, переключатель раскладок и недавние запросы. */
@Composable
private fun KeyboardColumn(
    state: SearchState,
    onEvent: (SearchEvent) -> Unit,
    focus: CatalogScreenFocus,
    exitRight: FocusRequester?,
) {
    val keys = remember(onEvent) {
        TvKeyboardActions(
            onKey = { key -> onEvent(SearchEvent.TypeKey(key)) },
            onSpace = { onEvent(SearchEvent.TypeKey(SPACE_KEY)) },
            onDelete = { onEvent(SearchEvent.DeleteLast) },
        )
    }
    Column(Modifier.width(TvKeyboardWidth)) {
        TvKeyboard(
            rows = keyboardRows(state.keyboardLayout),
            actions = keys,
            modifier = Modifier.catalogTarget(focus.jumps, CatalogTarget.KEYBOARD),
            exitRight = exitRight,
        )
        TvKeyboardLayouts(
            labels = KeyboardLayoutLabels.map { (_, label) -> label },
            selected = KeyboardLayoutLabels.indexOfFirst { (layout, _) -> layout == state.keyboardLayout },
            onSelect = { index -> onEvent(SearchEvent.SwitchLayout(KeyboardLayoutLabels[index].first)) },
            modifier = Modifier.padding(top = KeyboardToLayouts),
        )
        if (state.recentQueries.isNotEmpty()) {
            RecentQueries(state.recentQueries, onEvent, Modifier.padding(top = LayoutsToRecent))
        }
    }
}

/** Недавние запросы: «ОК» ищет сразу — и фокус уходит на найденное. */
@Composable
private fun RecentQueries(queries: List<String>, onEvent: (SearchEvent) -> Unit, modifier: Modifier) {
    Column(modifier) {
        TvOverline("Недавние", Modifier.padding(bottom = RecentTitleGap), accent = false)
        // Текст строк — по линии заголовка, подложка фокуса выступает влево.
        Column(Modifier.offset(x = -TvTextRowInset).tvFocusGroup()) {
            queries.forEach { query ->
                TvTextRow(text = query, onClick = { onEvent(SearchEvent.OpenSearch(query)) }, dense = true)
            }
        }
    }
}

/**
 * Правая колонка. Новая выдача сменяет прежнюю кроссфейдом 200 мс без изменения размеров: при
 * наборе по букве результаты не мигают. Пока набирают, фокус на клавиатуре, а лучшее совпадение
 * после голоса берёт фокус заявкой — новая выдача уже на экране, когда она исполняется.
 */
@Composable
private fun SearchResults(
    state: SearchState,
    onEvent: (SearchEvent) -> Unit,
    focus: CatalogScreenFocus,
    bestMatch: FocusRequester,
    modifier: Modifier,
) {
    val reduced = LocalReducedMotion.current
    AnimatedContent(
        targetState = state,
        modifier = modifier.fillMaxSize(),
        contentKey = { it.results },
        transitionSpec = { resultsTransition(reduced) },
        label = "searchResults",
    ) { shown ->
        ResultsContent(shown, onEvent, focus, bestMatch)
    }
}

private fun resultsTransition(reduced: Boolean): ContentTransform =
    if (reduced) {
        ContentTransform(EnterTransition.None, ExitTransition.None, sizeTransform = null)
    } else {
        ContentTransform(fadeIn(FxMotion.fade()), fadeOut(FxMotion.fade()), sizeTransform = null)
    }

/** Подсказка, пока искать нечего, или лучшее совпадение и остальные результаты. */
@Composable
private fun ResultsContent(
    state: SearchState,
    onEvent: (SearchEvent) -> Unit,
    focus: CatalogScreenFocus,
    bestMatch: FocusRequester,
) {
    val match = state.bestMatch
    if (match == null) {
        SearchHint(searchHint(state), Modifier)
        return
    }
    val highlight = LocalFxColors.current.accentText
    // Сетка поднята на запас под фокус: увеличенная карточка и свечение не срезаются верхним краем.
    LazyVerticalGrid(
        columns = GridCells.Fixed(RESULT_COLUMNS),
        modifier = Modifier.fillMaxSize().offset(y = -TvMetrics.FocusInset),
        contentPadding = PaddingValues(
            top = TvMetrics.FocusInset,
            bottom = TvMetrics.SafeVertical + TvMetrics.FocusInset,
        ),
        horizontalArrangement = Arrangement.spacedBy(ResultGap),
        verticalArrangement = Arrangement.spacedBy(ResultRowGap),
    ) {
        fullWidth("match-title") { TvOverline("Лучшее совпадение", accent = false) }
        fullWidth("match") {
            TvSpotlightCard(
                spotlight = bestMatchSpotlight(match, state.query, highlight),
                onClick = { onEvent(SearchEvent.OpenResult(match.id)) },
                modifier = Modifier
                    .focusRequester(bestMatch)
                    .catalogTarget(focus.jumps, CatalogTarget.BEST_MATCH)
                    .then(focus.screen.item(BEST_MATCH_KEY)),
            )
        }
        otherResults(state.otherResults, onEvent, focus)
    }
}

private fun LazyGridScope.otherResults(items: List<Item>, onEvent: (SearchEvent) -> Unit, focus: CatalogScreenFocus) {
    if (items.isEmpty()) return
    fullWidth("more-title") {
        TvOverline(moreResultsLabel(items.size), Modifier.padding(top = MoreResultsTop), accent = false)
    }
    // Мелкие постеры — чистые, как в макете: подпись и оценка на 106dp не читаются, а тайтл
    // называет лучшее совпадение над ними и озвучка — по названию на картинке.
    itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
        TvPosterCard(
            title = item.title,
            meta = null,
            posterUrl = posterUrl(item),
            onClick = { onEvent(SearchEvent.OpenResult(item.id)) },
            modifier = Modifier.fxEnter(index % RESULT_COLUMNS).then(focus.screen.item("result:${item.id}")),
            size = TvPosterSize.Compact,
            details = TvPosterDetails.None,
        )
    }
}

private fun LazyGridScope.fullWidth(key: String, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun SearchHint(text: String, modifier: Modifier) {
    Box(modifier.padding(top = 4.dp)) {
        Text(text, style = FxType.metaRow, color = LocalFxColors.current.textSecondary)
    }
}

/** Лучшее совпадение крупной карточкой: кадр, название с подсветкой, оценка, «Сериал · 2012 · Драма». */
internal fun bestMatchSpotlight(item: Item, query: String, highlight: Color): TvSpotlight = TvSpotlight(
    title = highlightedTitle(item.title, query, highlight),
    meta = listOfNotNull(posterMeta(typeLabel(item.type), item.year), item.genres.firstOrNull()?.title)
        .joinToString(" · ")
        .ifBlank { null },
    imageUrl = item.posters.wide ?: item.posters.big,
    rating = ratingLabel(item.rating.kinopoisk),
    ratingHighlighted = isHighRating(item.rating.kinopoisk),
)

/** Название с подсветкой совпадения с [query] цветом [highlight]; нет совпадения — как есть. */
internal fun highlightedTitle(title: String, query: String, highlight: Color): AnnotatedString {
    val match = matchRange(title, query) ?: return AnnotatedString(title)
    return buildAnnotatedString {
        append(title.substring(0, match.first))
        withStyle(SpanStyle(color = highlight)) { append(title.substring(match)) }
        append(title.substring(match.last + 1))
    }
}

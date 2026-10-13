// Режим поиска (макет S06): экранная клавиатура и недавние запросы слева, «Лучшее совпадение»
// крупной карточкой и остальные результаты постерами справа.
package com.filmax.feature.search.tv

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
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.tv.designsystem.FxType
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.TvKeyboard
import com.filmax.core.tv.designsystem.TvKeyboardActions
import com.filmax.core.tv.designsystem.TvKeyboardLayouts
import com.filmax.core.tv.designsystem.TvKeyboardWidth
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOverline
import com.filmax.core.tv.designsystem.TvPosterCard
import com.filmax.core.tv.designsystem.TvPosterSize
import com.filmax.core.tv.designsystem.TvSpotlightCard
import com.filmax.core.tv.designsystem.TvTextRow
import com.filmax.core.tv.designsystem.TvTextRowInset
import com.filmax.core.tv.designsystem.fxEnter
import com.filmax.core.tv.designsystem.tvFocusGroup
import com.filmax.core.ui.components.isHighRating
import com.filmax.core.ui.components.posterMeta
import com.filmax.core.ui.components.ratingLabel
import com.filmax.core.ui.components.typeLabel
import com.filmax.feature.search.common.KeyboardLayoutLabels
import com.filmax.feature.search.common.SPACE_KEY
import com.filmax.feature.search.common.SearchEvent
import com.filmax.feature.search.common.SearchState
import com.filmax.feature.search.common.keyboardRows

/** Результаты справа — по четыре постера 106dp с шагом 16dp на 470dp колонки. */
private const val RESULT_COLUMNS = 4
private val ResultGap = 16.dp

/** Отступы S06: строка поиска → колонки 26dp, клавиатура → результаты 40dp. */
private val BarToColumns = 26.dp
private val ColumnsGap = 40.dp
private val KeyboardToLayouts = 16.dp
private val LayoutsToRecent = 26.dp

@Composable
internal fun TvSearchPanel(state: SearchState, actions: CatalogActions, focus: CatalogScreenFocus) {
    val bestMatch = remember { FocusRequester() }
    Column(
        Modifier
            .fillMaxSize()
            .padding(start = TvMetrics.SectionStart, end = TvMetrics.SectionEnd, top = TvMetrics.ContentTop),
    ) {
        CatalogSearchBar(query = state.query, active = true, actions = actions, focus = focus)
        Row(Modifier.padding(top = BarToColumns)) {
            KeyboardColumn(
                state = state,
                dispatch = actions.dispatch,
                focus = focus,
                // «→» с правого края клавиатуры — на лучшее совпадение, а не на ближайший постер.
                exitRight = bestMatch.takeIf { state.bestMatch != null },
            )
            SearchResults(
                state = state,
                actions = actions,
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
    dispatch: (SearchEvent) -> Unit,
    focus: CatalogScreenFocus,
    exitRight: FocusRequester?,
) {
    val keys = remember(dispatch) {
        TvKeyboardActions(
            onKey = { key -> dispatch(SearchEvent.TypeKey(key)) },
            onSpace = { dispatch(SearchEvent.TypeKey(SPACE_KEY)) },
            onDelete = { dispatch(SearchEvent.DeleteLast) },
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
            onSelect = { index -> dispatch(SearchEvent.SwitchLayout(KeyboardLayoutLabels[index].first)) },
            modifier = Modifier.padding(top = KeyboardToLayouts),
        )
        if (state.recentQueries.isNotEmpty()) {
            RecentQueries(state.recentQueries, dispatch, Modifier.padding(top = LayoutsToRecent))
        }
    }
}

/** Недавние запросы: «ОК» ищет сразу — и фокус уходит на найденное. */
@Composable
private fun RecentQueries(queries: List<String>, dispatch: (SearchEvent) -> Unit, modifier: Modifier) {
    Column(modifier) {
        TvOverline("Недавние", Modifier.padding(bottom = 6.dp), accent = false)
        // Текст строк — по линии заголовка, подложка фокуса выступает влево.
        Column(Modifier.offset(x = -TvTextRowInset).tvFocusGroup()) {
            queries.forEach { query ->
                TvTextRow(text = query, onClick = { dispatch(SearchEvent.OpenSearch(query)) })
            }
        }
    }
}

/** Правая колонка: подсказка, пока искать нечего, или лучшее совпадение и остальные результаты. */
@Composable
private fun SearchResults(
    state: SearchState,
    actions: CatalogActions,
    focus: CatalogScreenFocus,
    bestMatch: FocusRequester,
    modifier: Modifier,
) {
    val match = state.bestMatch
    if (match == null) {
        SearchHint(searchHint(state), modifier)
        return
    }
    val highlight = LocalFxColors.current.accentText
    // Сетка поднята на запас под фокус: увеличенная карточка и свечение не срезаются верхним краем.
    LazyVerticalGrid(
        columns = GridCells.Fixed(RESULT_COLUMNS),
        modifier = modifier.fillMaxSize().offset(y = -TvMetrics.FocusInset),
        contentPadding = PaddingValues(
            top = TvMetrics.FocusInset,
            bottom = TvMetrics.SafeVertical + TvMetrics.FocusInset
        ),
        horizontalArrangement = Arrangement.spacedBy(ResultGap),
        verticalArrangement = Arrangement.spacedBy(ResultGap),
    ) {
        fullWidth("match-title") { TvOverline("Лучшее совпадение", accent = false) }
        fullWidth("match") {
            TvSpotlightCard(
                spotlight = bestMatchSpotlight(match, state.query, highlight),
                onClick = { actions.onOpenResult(match.id) },
                modifier = Modifier
                    .focusRequester(bestMatch)
                    .catalogTarget(focus.jumps, CatalogTarget.BEST_MATCH)
                    .then(focus.screen.item(BEST_MATCH_KEY)),
            )
        }
        otherResults(state.otherResults, actions, focus)
    }
}

private fun LazyGridScope.otherResults(items: List<Item>, actions: CatalogActions, focus: CatalogScreenFocus) {
    if (items.isEmpty()) return
    fullWidth("more-title") { TvOverline(moreResultsLabel(items.size), Modifier.padding(top = 6.dp), accent = false) }
    itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
        TvPosterCard(
            title = item.title,
            meta = posterMeta(typeLabel(item.type), item.year),
            posterUrl = posterUrl(item),
            onClick = { actions.onOpenResult(item.id) },
            modifier = Modifier.fxEnter(index % RESULT_COLUMNS).then(focus.screen.item("result:${item.id}")),
            rating = ratingLabel(item.rating.kinopoisk),
            ratingHighlighted = isHighRating(item.rating.kinopoisk),
            size = TvPosterSize.Compact,
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

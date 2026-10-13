// Шапка витрины — первый элемент сетки: строка поиска, чипы типа с сортировкой и фильтрами,
// чипы жанров и сводка выборки. Строка поиска общая с режимом поиска.
package com.filmax.feature.search.tv

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.tv.designsystem.FxMotion
import com.filmax.core.tv.designsystem.FxType
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.LocalReducedMotion
import com.filmax.core.tv.designsystem.TvChip
import com.filmax.core.tv.designsystem.TvChipStyle
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvMicButton
import com.filmax.core.tv.designsystem.TvSearchField
import com.filmax.core.tv.designsystem.TvSearchFieldMode
import com.filmax.core.tv.designsystem.tvFocusGroup
import com.filmax.feature.search.common.CatalogSheet
import com.filmax.feature.search.common.SearchEvent
import com.filmax.feature.search.common.SearchState
import com.filmax.feature.search.common.TypeOptions

private const val SEARCH_PLACEHOLDER = "Название фильма или сериала"

private val ChipGap = 8.dp
private val SearchToChips = 16.dp
private val TypesToGenres = 10.dp
private val GenresToSummary = 18.dp
private val DividerHeight = 24.dp
private const val DIVIDER_ALPHA = 0.14f

@Composable
internal fun CatalogHeader(
    state: SearchState,
    actions: CatalogActions,
    focus: CatalogScreenFocus,
    sheetOpeners: SheetOpeners,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        CatalogSearchBar(
            query = "",
            active = false,
            actions = actions,
            focus = focus,
            fieldModifier = focus.screen.item(SEARCH_KEY).catalogTarget(focus.jumps, CatalogTarget.SEARCH_FIELD),
        )
        // «Вниз» с чипов типа — в ряд жанров явно. Ряд жанров во всю ширину, и для поиска фокуса по
        // направлению его центр далеко справа: с левых чипов «вниз» перескакивал жанры прямо в сетку.
        val genreRow = remember { FocusRequester() }
        val hasGenres = state.genres.isNotEmpty()
        TypeRow(
            state = state,
            dispatch = actions.dispatch,
            openers = sheetOpeners,
            below = genreRow.takeIf { hasGenres },
            modifier = Modifier.padding(top = SearchToChips),
        )
        if (hasGenres) {
            GenreRow(
                genres = state.genres,
                selectedId = state.selectedGenreId,
                dispatch = actions.dispatch,
                modifier = Modifier.padding(top = TypesToGenres).focusRequester(genreRow),
            )
        }
        CatalogSummary(catalogSummary(state), Modifier.padding(top = GenresToSummary))
    }
}

/**
 * Сводка выборки «Фантастика · 42 результата». Меняется вместе с выдачей при смене фильтра —
 * кроссфейдом, в том же темпе, в каком старая сетка гаснет, а новая появляется.
 */
@Composable
private fun CatalogSummary(summary: String, modifier: Modifier) {
    val reduced = LocalReducedMotion.current
    Crossfade(summary, modifier, animationSpec = if (reduced) snap() else FxMotion.fade(), label = "summary") { text ->
        Text(text, style = FxType.meta, color = LocalFxColors.current.textDim)
    }
}

/**
 * Строка поиска: поле и кнопка голоса. На витрине поле — кнопка, открывающая поиск; в поиске —
 * [active]: показывает набранное, а фокус остаётся на клавиатуре.
 */
@Composable
internal fun CatalogSearchBar(
    query: String,
    active: Boolean,
    actions: CatalogActions,
    focus: CatalogScreenFocus,
    fieldModifier: Modifier = Modifier,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TvSearchField(
            query = query,
            placeholder = SEARCH_PLACEHOLDER,
            onClick = actions.onOpenSearch,
            mode = if (active) TvSearchFieldMode.Typing else TvSearchFieldMode.Button,
            modifier = fieldModifier.weight(1f),
        )
        TvMicButton(onClick = actions.onVoice, modifier = Modifier.catalogTarget(focus.jumps, CatalogTarget.MIC))
    }
}

/**
 * Чипы типа, затем сортировка и фильтры — они открывают боковые панели. Ряд — группа фокуса:
 * первый вход — на выбранный тип, возврат — на чип, с которого ушли. [below] — куда «вниз» с любого
 * чипа; свойство стоит на каждом чипе: focusProperties ряда на детей не распространяется.
 */
@Composable
private fun TypeRow(
    state: SearchState,
    dispatch: (SearchEvent) -> Unit,
    openers: SheetOpeners,
    below: FocusRequester?,
    modifier: Modifier,
) {
    val selectedType = remember { FocusRequester() }
    val chip = Modifier.focusProperties { below?.let { down = it } }
    Row(
        modifier.tvFocusGroup(fallback = selectedType),
        horizontalArrangement = Arrangement.spacedBy(ChipGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TypeOptions.forEach { (type, label) ->
            val selected = state.filter == type
            TvChip(
                label = label,
                selected = selected,
                onClick = { dispatch(SearchEvent.FilterChange(type)) },
                modifier = if (selected) chip.focusRequester(selectedType) else chip,
            )
        }
        ChipDivider()
        TvChip(
            label = sortChipLabel(state.sort),
            selected = false,
            onClick = { dispatch(SearchEvent.OpenSheet(CatalogSheet.SORT)) },
            leadingIcon = Icons.Rounded.SwapVert,
            trailingIcon = Icons.Rounded.ExpandMore,
            modifier = chip.focusRequester(openers.sort),
        )
        TvChip(
            label = filtersChipLabel(state.filters),
            selected = state.filters.activeCount > 0,
            onClick = { dispatch(SearchEvent.OpenSheet(CatalogSheet.FILTERS)) },
            leadingIcon = Icons.Rounded.Tune,
            modifier = chip.focusRequester(openers.filters),
        )
    }
}

/** Тонкая черта между типами и сортировкой: это разные решения, и ряд читается двумя группами. */
@Composable
private fun ChipDivider() {
    Box(
        Modifier
            .padding(horizontal = 4.dp)
            .width(1.dp)
            .height(DividerHeight)
            .background(Color.White.copy(alpha = DIVIDER_ALPHA)),
    )
}

/**
 * Жанры контурными чипами в горизонтальном списке — их десятки. Повторный «ОК» по выбранному
 * снимает фильтр: отдельного чипа «Все жанры» в ряду нет.
 */
@Composable
private fun GenreRow(genres: List<Genre>, selectedId: Int?, dispatch: (SearchEvent) -> Unit, modifier: Modifier) {
    val firstGenre = remember { FocusRequester() }
    LazyRow(
        // Список шире колонки на запас под фокус: увеличенный крайний чип и его свечение не
        // срезаются краем списка, а первый чип стоит на линии остальной шапки.
        // Запрошенный фокус ([modifier] с focusRequester) входит в ряд через его память: на жанр, с
        // которого ушли, или на первый.
        modifier = Modifier.outsetHorizontally(TvMetrics.FocusInset).then(modifier).tvFocusGroup(fallback = firstGenre),
        contentPadding = PaddingValues(horizontal = TvMetrics.FocusInset),
        horizontalArrangement = Arrangement.spacedBy(ChipGap),
    ) {
        itemsIndexed(genres, key = { _, genre -> genre.id }) { index, genre ->
            val selected = genre.id == selectedId
            TvChip(
                label = genre.title,
                selected = selected,
                onClick = { dispatch(SearchEvent.GenreChange(if (selected) null else genre.id)) },
                style = TvChipStyle.Outlined,
                modifier = if (index == 0) Modifier.focusRequester(firstGenre) else Modifier,
            )
        }
    }
}

/** Элемент шире своего места на [outset] с каждой стороны — без сдвига соседей. */
private fun Modifier.outsetHorizontally(outset: Dp): Modifier = layout { measurable, constraints ->
    val outsetPx = outset.roundToPx()
    val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + outsetPx * 2))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-outsetPx, 0) }
}

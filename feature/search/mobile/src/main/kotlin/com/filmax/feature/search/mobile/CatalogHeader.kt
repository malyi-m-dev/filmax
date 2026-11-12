// Стеклянная шапка каталога: заголовок с сортировкой и фильтрами, сегменты типа и чипы выборки
// (сортировка, применённые фильтры, жанр). Сетка и состояние экрана — в CatalogScreen.

package com.filmax.feature.search.mobile

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.GlassState
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.ui.components.FilmaxChip
import com.filmax.core.ui.components.FilmaxGlassHeader
import com.filmax.core.ui.components.FilmaxIconButton
import com.filmax.core.ui.components.FilmaxSegmentedControl
import com.filmax.feature.search.common.CatalogSheet
import com.filmax.feature.search.common.SearchEvent
import com.filmax.feature.search.common.SearchState
import com.filmax.feature.search.common.TypeOptions
import com.filmax.feature.search.common.appliedFilters
import com.filmax.feature.search.common.genrePickable
import com.filmax.feature.search.common.selectedGenre
import com.filmax.feature.search.common.sortLabel
import com.filmax.feature.search.common.title

/**
 * Типы сегментами. Подпись документального короче общей: пять сегментов в одной дорожке на узком
 * телефоне вмещают «Докум.», но не «Документальные».
 */
private val SegmentOptions: List<Pair<ItemType?, String>> = TypeOptions.map { (type, label) ->
    type to if (type == ItemType.DOCUMENTARY) "Докум." else label
}

@Composable
internal fun CatalogHeader(
    state: SearchState,
    actions: CatalogActions,
    glass: GlassState,
    modifier: Modifier = Modifier,
) {
    FilmaxGlassHeader(glass = glass, modifier = modifier) {
        // «Назад» — только у раздела, открытого с Главной: вкладке Каталога возвращаться некуда.
        TitleRow(
            title = state.title,
            onBack = actions.onBack.takeIf { state.section != null },
            onEvent = actions.onEvent,
        )
        val selectedSegment = SegmentOptions.indexOfFirst { it.first == state.filter }
        // Раздел вне сегментов («ТВ-шоу», «Концерты» из «Разделов»): ни один сегмент такую выборку
        // не описывает, а подсвеченное «Все» соврало бы. Тип здесь задал раздел, и его называет заголовок.
        if (selectedSegment >= 0) {
            FilmaxSegmentedControl(
                options = SegmentOptions.map { it.second },
                selectedIndex = selectedSegment,
                onSelect = { index -> actions.onEvent(SearchEvent.FilterChange(SegmentOptions[index].first)) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = FilmaxMetrics.GridPadding),
            )
        }
        SelectionChips(state = state, onEvent = actions.onEvent)
    }
}

@Composable
private fun TitleRow(title: String, onBack: (() -> Unit)?, onEvent: (SearchEvent) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(FilmaxMetrics.HeaderRowHeight)
            .padding(start = if (onBack != null) 12.dp else FilmaxMetrics.ScreenPadding, end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        onBack?.let {
            FilmaxIconButton(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                onClick = it,
                size = FilmaxMetrics.IconButtonSmall,
            )
        }
        Text(
            title,
            style = FilmaxType.screenTitle,
            color = FilmaxColors.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        FilmaxIconButton(
            Icons.Filled.SwapVert,
            contentDescription = "Сортировка",
            onClick = { onEvent(SearchEvent.OpenSheet(CatalogSheet.SORT)) },
            size = FilmaxMetrics.IconButtonSmall,
        )
        FilmaxIconButton(
            Icons.Filled.Tune,
            contentDescription = "Фильтры",
            onClick = { onEvent(SearchEvent.OpenSheet(CatalogSheet.FILTERS)) },
            size = FilmaxMetrics.IconButtonSmall,
        )
    }
}

/**
 * Выборка чипами: сортировка, применённые фильтры с «✕» (акцентом — их видно сразу) и жанр.
 * Тап по чипу сортировки или жанра — шторка с вариантами.
 */
@Composable
private fun SelectionChips(state: SearchState, onEvent: (SearchEvent) -> Unit) {
    val genre = state.selectedGenre
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = FilmaxMetrics.ScreenPadding)
            .padding(top = 12.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilmaxChip(
            label = sortLabel(state.sort.field),
            selected = false,
            onClick = { onEvent(SearchEvent.OpenSheet(CatalogSheet.SORT)) },
            trailingIcon = Icons.Filled.KeyboardArrowDown,
        )
        state.appliedFilters.forEach { filter ->
            FilmaxChip(
                label = filter.label,
                selected = true,
                onClick = { onEvent(SearchEvent.ApplyFilters(filter.without)) },
                trailingIcon = Icons.Filled.Close,
            )
        }
        if (genre != null) {
            FilmaxChip(
                label = genre.title,
                selected = true,
                onClick = { onEvent(SearchEvent.GenreChange(null)) },
                trailingIcon = Icons.Filled.Close
            )
        } else if (state.genrePickable) {
            FilmaxChip(
                label = "Жанр",
                selected = false,
                onClick = { onEvent(SearchEvent.OpenSheet(CatalogSheet.GENRE)) },
                trailingIcon = Icons.Filled.KeyboardArrowDown,
            )
        }
        if (state.filters.activeCount == 0) {
            FilmaxChip(
                label = "Год и рейтинг",
                selected = false,
                onClick = { onEvent(SearchEvent.OpenSheet(CatalogSheet.FILTERS)) },
                trailingIcon = Icons.Filled.KeyboardArrowDown,
            )
        }
    }
}

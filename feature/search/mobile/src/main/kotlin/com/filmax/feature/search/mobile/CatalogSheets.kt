// Шторки каталога: какая открыта — по состоянию модели; сортировка и жанр — здесь, выбор в них
// применяется сразу. Фильтры копятся черновиком до «Применить» — в CatalogFiltersSheet.

package com.filmax.feature.search.mobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.presentation.format.title
import com.filmax.core.ui.components.FilmaxRadioRow
import com.filmax.core.ui.components.FilmaxSegmentedControl
import com.filmax.core.ui.components.FilmaxSheet
import com.filmax.feature.search.common.CatalogSheet
import com.filmax.feature.search.common.SearchEvent
import com.filmax.feature.search.common.SearchState
import com.filmax.feature.search.common.sortOptions

/**
 * Открытая шторка — по [SearchState.sheet]. Выбор шлёт событие и уводит шторку вниз; закрытие
 * ([SearchEvent.CloseSheet]) модель получает, когда шторка доехала, — так она уходит с анимацией.
 */
@Composable
internal fun CatalogSheetHost(state: SearchState, onEvent: (SearchEvent) -> Unit) {
    val onDismiss = { onEvent(SearchEvent.CloseSheet) }
    when (state.sheet) {
        CatalogSheet.SORT -> SortSheet(
            sort = state.sort,
            options = state.capabilities.sortOptions(),
            onSort = { onEvent(SearchEvent.SortChange(it)) },
            onDismiss = onDismiss,
        )
        CatalogSheet.GENRE -> GenreSheet(
            state = state,
            onGenre = { onEvent(SearchEvent.GenreChange(it)) },
            onDismiss = onDismiss,
        )
        CatalogSheet.FILTERS -> FiltersSheet(state = state, onEvent = onEvent, onDismiss = onDismiss)
    }
}

/** Поле сортировки — радиокнопками, направление — сегментами над ними. */
@Composable
private fun SortSheet(
    sort: SortOption,
    options: List<Pair<CatalogSort, String>>,
    onSort: (SortOption) -> Unit,
    onDismiss: () -> Unit,
) {
    FilmaxSheet(onDismiss = onDismiss, title = "Сортировка") {
        val sheet = this
        Column(
            Modifier.padding(horizontal = FilmaxMetrics.GridPadding).padding(bottom = 24.dp).navigationBarsPadding()
        ) {
            FilmaxSegmentedControl(
                options = listOf("По убыванию", "По возрастанию"),
                selectedIndex = if (sort.ascending) 1 else 0,
                onSelect = { index -> onSort(sort.copy(ascending = index == 1)) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )
            options.forEach { (field, label) ->
                FilmaxRadioRow(
                    label = label,
                    selected = field == sort.field,
                    onClick = {
                        onSort(SortOption(field, sort.ascending))
                        sheet.dismiss()
                    },
                )
            }
        }
    }
}

/** Жанры кино и сериалов из справочника источника; «Любой» снимает фильтр. Выбор закрывает шторку. */
@Composable
private fun GenreSheet(state: SearchState, onGenre: (Int?) -> Unit, onDismiss: () -> Unit) {
    FilmaxSheet(onDismiss = onDismiss, title = "Жанр") {
        val sheet = this
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .heightIn(max = GenreListHeight)
                .padding(horizontal = FilmaxMetrics.GridPadding),
        ) {
            item(key = "any") {
                FilmaxRadioRow(
                    label = "Любой",
                    selected = state.selectedGenreId == null,
                    onClick = {
                        onGenre(null)
                        sheet.dismiss()
                    },
                )
            }
            items(state.genres, key = { it.id }) { genre ->
                FilmaxRadioRow(
                    label = genre.title,
                    selected = genre.id == state.selectedGenreId,
                    onClick = {
                        onGenre(genre.id)
                        sheet.dismiss()
                    },
                )
            }
            item(key = "bottom") { Box(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) }
        }
    }
}

private val GenreListHeight = 480.dp

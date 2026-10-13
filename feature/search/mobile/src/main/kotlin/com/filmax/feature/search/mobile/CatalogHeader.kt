// Шапка каталога: заголовок с кнопкой фильтров и сортировкой, строка поиска, чипы типа и
// жанров. Сетка и состояние экрана — в CatalogScreen, сама строка поиска — в CatalogSearchBar.

package com.filmax.feature.search.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.ShapeFull
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.feature.search.common.SearchState
import com.filmax.feature.search.common.SortOptions
import com.filmax.feature.search.common.TypeOptions
import com.filmax.feature.search.common.sortLabel

@Composable
internal fun CatalogHeader(state: SearchState, actions: CatalogActions) {
    Column {
        CatalogTitleRow(
            sort = state.sort,
            filterCount = state.filters.activeCount,
            onSort = actions.onSort,
            onOpenFilters = actions.onOpenFilters,
        )
        Spacer(Modifier.height(16.dp))
        CatalogSearchButton(query = state.query, onClick = actions.onOpenSearch)
        Spacer(Modifier.height(16.dp))
        CatalogTypeRow(filter = state.filter, onFilter = actions.onFilter)
        if (state.genres.isNotEmpty()) {
            Spacer(Modifier.height(11.dp))
            CatalogGenreRow(
                genres = state.genres,
                selectedId = state.selectedGenreId,
                onGenre = actions.onGenre,
            )
        }
        Spacer(Modifier.height(6.dp))
    }
}

/** Сортировка и фильтры живут в строке заголовка: место рядом с «Каталогом» в макете пустует. */
@Composable
private fun CatalogTitleRow(
    sort: SortOption,
    filterCount: Int,
    onSort: (SortOption) -> Unit,
    onOpenFilters: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Каталог",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterButton(count = filterCount, onClick = onOpenFilters)
            SortControl(sort = sort, onSort = onSort)
        }
    }
}

/** Кнопка листа фильтров с бейджем: число активных фильтров прямо на иконке. */
@Composable
private fun FilterButton(count: Int, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.TopEnd) {
        Row(
            modifier = Modifier
                .height(FilmaxMetrics.GenreChipHeight)
                .clip(ShapeFull)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                Icons.Filled.Tune,
                contentDescription = "Фильтры",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp),
            )
            Text(
                "Фильтры",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (count > 0) {
            Box(
                modifier = Modifier
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(16.dp)
                    .clip(ShapeFull)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Выбор поля сортировки (меню) плюс стрелка направления рядом. */
@Composable
private fun SortControl(sort: SortOption, onSort: (SortOption) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SortFieldChip(field = sort.field) { field -> onSort(SortOption(field, sort.ascending)) }
        DirectionToggle(ascending = sort.ascending) { onSort(sort.copy(ascending = !sort.ascending)) }
    }
}

/**
 * Выпадающее меню полей, а не перебор по кругу (как на пульте): пальцем вариант выбирают из
 * списка, и текущее поле должно быть видно вместе с остальными.
 */
@Composable
private fun SortFieldChip(field: CatalogSort, onPick: (CatalogSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .height(FilmaxMetrics.GenreChipHeight)
                .clip(ShapeFull)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable { expanded = true }
                .padding(start = 14.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                sortLabel(field),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                Icons.Filled.ExpandMore,
                contentDescription = "Сортировка",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortOptions.forEach { (option, label) ->
                SortMenuItem(
                    label = label,
                    selected = option == field,
                    onClick = {
                        expanded = false
                        onPick(option)
                    },
                )
            }
        }
    }
}

/** Стрелка направления: вверх — по возрастанию, вниз — по убыванию (kino.pub `-field`). */
@Composable
private fun DirectionToggle(ascending: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(FilmaxMetrics.GenreChipHeight)
            .clip(ShapeFull)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (ascending) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
            contentDescription = if (ascending) "По возрастанию" else "По убыванию",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
private fun SortMenuItem(label: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        onClick = onClick,
        trailingIcon = {
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        },
    )
}

@Composable
private fun CatalogTypeRow(filter: ItemType?, onFilter: (ItemType?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        items(TypeOptions) { (type, label) ->
            CatalogChip(
                label = label,
                selected = filter == type,
                onClick = { onFilter(type) },
                modifier = Modifier.height(FilmaxMetrics.ChipHeight),
            )
        }
    }
}

@Composable
private fun CatalogGenreRow(genres: List<Genre>, selectedId: Int?, onGenre: (Int?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        items(genres, key = { it.id }) { genre ->
            CatalogChip(
                label = genre.title,
                selected = genre.id == selectedId,
                // Повторный тап по выбранному жанру снимает фильтр — отдельного чипа «Все» в ряду нет.
                onClick = { onGenre(if (genre.id == selectedId) null else genre.id) },
                modifier = Modifier.height(FilmaxMetrics.GenreChipHeight),
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            )
        }
    }
}

/**
 * Чип каталога. Выбранный — белая заливка с тёмным текстом: в монохроме заливка и есть
 * единственный маркер выбора, галочек и рамок макет не знает.
 */
@Composable
private fun CatalogChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    Box(
        modifier = modifier
            .clip(ShapeFull)
            .background(if (selected) MaterialTheme.colorScheme.primary else containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
        )
    }
}

@file:OptIn(ExperimentalMaterial3Api::class)

package com.filmax.feature.search.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxOnSurfaceDim
import com.filmax.core.designsystem.ShapeButton
import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.model.Country
import java.time.Year

/** Нижняя граница диапазона года: раньше кино как индустрии по сути нет. */
private const val MIN_YEAR = 1902

/** Верх шкалы рейтинга — по обеим внешним оценкам kino.pub 0–10. */
private const val RATING_MAX = 10

/**
 * Лист расширенных фильтров каталога (экран «Каталог»). Правки копятся в черновике и уходят в
 * ScreenModel только по «Применить» — тянуть сеть на каждый сдвиг слайдера незачем.
 */
@Composable
internal fun CatalogFilterSheet(
    filters: CatalogFilters,
    countries: List<Country>,
    onApply: (CatalogFilters) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        FilterSheetContent(
            initial = filters,
            countries = countries,
            onApply = onApply,
            onReset = onReset,
        )
    }
}

@Composable
private fun FilterSheetContent(
    initial: CatalogFilters,
    countries: List<Country>,
    onApply: (CatalogFilters) -> Unit,
    onReset: () -> Unit,
) {
    var draft by remember { mutableStateOf(initial) }
    val maxYear = remember { Year.now().value }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FilmaxMetrics.ScreenPadding)
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("Фильтры", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        YearSection(from = draft.yearFrom, to = draft.yearTo, maxYear = maxYear) { from, to ->
            draft = draft.copy(yearFrom = from, yearTo = to)
        }
        RatingSection(title = "Рейтинг Кинопоиска", value = draft.kpRatingFrom) {
            draft = draft.copy(kpRatingFrom = it)
        }
        RatingSection(title = "Рейтинг IMDb", value = draft.imdbRatingFrom) {
            draft = draft.copy(imdbRatingFrom = it)
        }
        CountrySection(countries = countries, selectedId = draft.countryId) {
            draft = draft.copy(countryId = it)
        }
        ToggleRow(title = "Только 4K", checked = draft.only4k) { draft = draft.copy(only4k = it) }
        ToggleRow(title = "Только завершённые", checked = draft.onlyFinished == true) { on ->
            // Тумблер бинарный: включён — только завершённые, выключен — любые. Значение false
            // (только продолжающиеся) на телефоне не выставляем, домен его допускает для TV.
            draft = draft.copy(onlyFinished = if (on) true else null)
        }
        SheetActions(onApply = { onApply(draft) }, onReset = onReset)
    }
}

@Composable
private fun YearSection(from: Int?, to: Int?, maxYear: Int, onChange: (Int?, Int?) -> Unit) {
    val committedStart = (from ?: MIN_YEAR).toFloat()
    val committedEnd = (to ?: maxYear).toFloat()
    var range by remember(from, to, maxYear) { mutableStateOf(committedStart..committedEnd) }

    SectionLabel("Год выпуска", "${range.start.toInt()}–${range.endInclusive.toInt()}")
    RangeSlider(
        value = range,
        onValueChange = { range = it },
        onValueChangeFinished = {
            val low = range.start.toInt()
            val high = range.endInclusive.toInt()
            // Крайние значения = «не ограничивать»: не тащим в запрос условие «от 1902» и «до сейчас».
            onChange(low.takeIf { it > MIN_YEAR }, high.takeIf { it < maxYear })
        },
        valueRange = MIN_YEAR.toFloat()..maxYear.toFloat(),
    )
}

@Composable
private fun RatingSection(title: String, value: Int?, onChange: (Int?) -> Unit) {
    var current by remember(value) { mutableStateOf((value ?: 0).toFloat()) }
    val label = if (current < 1f) "любой" else "от ${current.toInt()}"

    SectionLabel(title, label)
    Slider(
        value = current,
        onValueChange = { current = it },
        onValueChangeFinished = { onChange(current.toInt().takeIf { it >= 1 }) },
        valueRange = 0f..RATING_MAX.toFloat(),
        steps = RATING_MAX - 1,
    )
}

@Composable
private fun CountrySection(countries: List<Country>, selectedId: Int?, onSelect: (Int?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedTitle = countries.firstOrNull { it.id == selectedId }?.title ?: "Любая"

    SectionLabel("Страна", null)
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ShapeButton)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(enabled = countries.isNotEmpty()) { expanded = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                selectedTitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Icon(
                Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CountryMenuItem(label = "Любая", selected = selectedId == null) {
                onSelect(null)
                expanded = false
            }
            countries.forEach { country ->
                CountryMenuItem(label = country.title, selected = country.id == selectedId) {
                    onSelect(country.id)
                    expanded = false
                }
            }
        }
    }
}

@Composable
private fun CountryMenuItem(label: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
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
private fun ToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SheetActions(onApply: () -> Unit, onReset: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) { Text("Сбросить") }
        Button(onClick = onApply, modifier = Modifier.weight(1f)) { Text("Применить") }
    }
}

/** Строка «название фильтра — текущее значение» над слайдером/списком. */
@Composable
private fun SectionLabel(title: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        if (value != null) {
            Text(value, style = MaterialTheme.typography.labelLarge, color = FilmaxOnSurfaceDim)
        }
    }
}

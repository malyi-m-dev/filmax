// Шторка фильтров Каталога: год и пороги оценок ползунками, страна, 4K и завершённость. Правки
// копятся черновиком модели (SearchState.filtersDraft) до «Применить».
package com.filmax.feature.search.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled._4k
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.model.Country
import com.filmax.core.presentation.format.title
import com.filmax.core.ui.components.FilmaxButton
import com.filmax.core.ui.components.FilmaxButtonStyle
import com.filmax.core.ui.components.FilmaxSettingsDivider
import com.filmax.core.ui.components.FilmaxSettingsGroup
import com.filmax.core.ui.components.FilmaxSettingsRow
import com.filmax.core.ui.components.FilmaxSheet
import com.filmax.core.ui.components.FilmaxToggleRow
import com.filmax.core.ui.components.filmaxSliderColors
import com.filmax.feature.search.common.SearchEvent
import com.filmax.feature.search.common.SearchState
import java.time.Year

/** Нижняя граница диапазона года: раньше кино как индустрии по сути нет. */
private const val MIN_YEAR = 1902

/** Верх шкалы рейтинга — по обеим внешним оценкам kino.pub 0–10. */
private const val RATING_MAX = 10

/**
 * Фильтры каталога. Правки копятся черновиком в модели ([SearchState.filtersDraft]) и уходят в выдачу
 * только по «Применить» — тянуть сеть на каждый сдвиг ползунка незачем; «Сбросить» применяет пустые
 * фильтры сразу.
 */
@Composable
internal fun FiltersSheet(state: SearchState, onEvent: (SearchEvent) -> Unit, onDismiss: () -> Unit) {
    val draft = state.filtersDraft
    val edit: (CatalogFilters) -> Unit = { onEvent(SearchEvent.EditFilters(it)) }
    FilmaxSheet(onDismiss = onDismiss, title = "Фильтры") {
        val sheet = this
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FilmaxMetrics.ScreenPadding)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            YearSection(draft) { from, to -> edit(draft.copy(yearFrom = from, yearTo = to)) }
            // Пороги — только по оценкам, которые есть у источника.
            state.capabilities.ratings.forEach { source ->
                RatingSection(
                    title = "Рейтинг ${source.title}",
                    value = draft.minRating(source),
                ) { edit(draft.withMinRating(source, it)) }
            }
            // Страну не предлагаем источнику, который по ней не фильтрует.
            FilterToggles(
                draft = draft,
                countries = state.countries.takeIf { state.capabilities.countries }.orEmpty(),
                onChange = edit,
            )
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilmaxButton(
                    text = "Сбросить",
                    onClick = {
                        onEvent(SearchEvent.ResetFilters)
                        sheet.dismiss()
                    },
                    style = FilmaxButtonStyle.Secondary,
                    modifier = Modifier.weight(1f),
                )
                FilmaxButton(
                    text = "Применить",
                    onClick = {
                        onEvent(SearchEvent.ApplyDraftFilters)
                        sheet.dismiss()
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun YearSection(draft: CatalogFilters, onChange: (Int?, Int?) -> Unit) {
    val maxYear = remember { Year.now().value }
    var range by remember(draft.yearFrom, draft.yearTo) {
        mutableStateOf((draft.yearFrom ?: MIN_YEAR).toFloat()..(draft.yearTo ?: maxYear).toFloat())
    }
    Column {
        SectionLabel("Год выпуска", "${range.start.toInt()}–${range.endInclusive.toInt()}")
        RangeSlider(
            value = range,
            onValueChange = { range = it },
            onValueChangeFinished = {
                // Крайние значения = «не ограничивать»: не тащим в запрос условие «от 1902» и «до сейчас».
                onChange(
                    range.start.toInt().takeIf { it > MIN_YEAR },
                    range.endInclusive.toInt().takeIf { it < maxYear }
                )
            },
            valueRange = MIN_YEAR.toFloat()..maxYear.toFloat(),
            colors = filmaxSliderColors(),
        )
    }
}

@Composable
private fun RatingSection(title: String, value: Int?, onChange: (Int?) -> Unit) {
    var current by remember(value) { mutableStateOf((value ?: 0).toFloat()) }
    Column {
        SectionLabel(title, if (current < 1f) "любой" else "от ${current.toInt()}")
        Slider(
            value = current,
            onValueChange = { current = it },
            onValueChangeFinished = { onChange(current.toInt().takeIf { it >= 1 }) },
            valueRange = 0f..RATING_MAX.toFloat(),
            steps = RATING_MAX - 1,
            colors = filmaxSliderColors(),
        )
    }
}

/** Страна, 4K и завершённость — одной группой строк, как настройки. */
@Composable
private fun FilterToggles(draft: CatalogFilters, countries: List<Country>, onChange: (CatalogFilters) -> Unit) {
    FilmaxSettingsGroup(title = null) {
        if (countries.isNotEmpty()) {
            CountryRow(countries = countries, selectedId = draft.countryId) { onChange(draft.copy(countryId = it)) }
            FilmaxSettingsDivider()
        }
        FilmaxToggleRow(
            Icons.Filled._4k,
            "Только 4K",
            checked = draft.only4k,
            onCheckedChange = { onChange(draft.copy(only4k = it)) }
        )
        FilmaxSettingsDivider()
        // Тумблер бинарный: включён — только завершённые, выключен — любые. Значение false
        // (только продолжающиеся) на телефоне не выставляем, домен его допускает для TV.
        FilmaxToggleRow(
            Icons.Filled.DoneAll,
            "Только завершённые",
            checked = draft.onlyFinished == true,
            onCheckedChange = { on -> onChange(draft.copy(onlyFinished = if (on) true else null)) },
        )
    }
}

@Composable
private fun CountryRow(countries: List<Country>, selectedId: Int?, onSelect: (Int?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilmaxSettingsRow(
            icon = Icons.Filled.Public,
            label = "Страна",
            value = countries.firstOrNull { it.id == selectedId }?.title ?: "Любая",
            onClick = { if (countries.isNotEmpty()) expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            (listOf<Country?>(null) + countries).forEach { country ->
                DropdownMenuItem(
                    text = { Text(country?.title ?: "Любая", style = FilmaxType.label, color = FilmaxColors.Text) },
                    onClick = {
                        onSelect(country?.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** Строка «название фильтра — текущее значение» над ползунком. */
@Composable
private fun SectionLabel(title: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = FilmaxType.chip, color = FilmaxColors.Text)
        Text(value, style = FilmaxType.chip, color = FilmaxColors.TextDim)
    }
}

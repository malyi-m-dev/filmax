// Боковые панели каталога: сортировка (выбор применяется сразу) и фильтры (черновик в модели
// до «Применить»). Хост панели — TvSheetHost в TvCatalogScreen.
@file:OptIn(ExperimentalLayoutApi::class)

package com.filmax.feature.search.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.Country
import com.filmax.core.tv.designsystem.FxType
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.TvButton
import com.filmax.core.tv.designsystem.TvButtonStyle
import com.filmax.core.tv.designsystem.TvChip
import com.filmax.core.tv.designsystem.TvChipStyle
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOverline
import com.filmax.core.tv.designsystem.TvTextRow
import com.filmax.core.tv.designsystem.TvTextRowInset
import com.filmax.core.tv.designsystem.tvFocusGroup
import com.filmax.feature.search.common.CatalogSheet
import com.filmax.feature.search.common.SearchEvent
import com.filmax.feature.search.common.SearchState
import com.filmax.feature.search.common.SortOptions

/** Поля панели — как у панели описания в деталях. */
private val SheetPadding = PaddingValues(start = 34.dp, end = 40.dp, top = 30.dp, bottom = 30.dp)
private val SheetTitle = FxType.screenTitle.copy(fontSize = 26.sp, lineHeight = 30.sp)
private val SectionGap = 22.dp
private val ChipGap = 8.dp

/** Содержимое боковой панели — сортировка или фильтры, по [SearchState.sheet]. */
@Composable
internal fun CatalogSheetContent(state: SearchState, dispatch: (SearchEvent) -> Unit) {
    when (state.sheet) {
        CatalogSheet.SORT -> SortSheet(state.sort, dispatch)
        CatalogSheet.FILTERS -> FiltersSheet(state.filtersDraft, state.countries, dispatch)
    }
}

/**
 * Сортировка: поле списком и порядок чипами. Выбор применяется сразу и закрывает панель — сравнивать
 * тут нечего, а лишнее «Применить» на пульте — лишнее нажатие. Первый вход фокуса — на текущее поле.
 */
@Composable
private fun SortSheet(sort: SortOption, dispatch: (SearchEvent) -> Unit) {
    val current = remember { FocusRequester() }
    Column(Modifier.fillMaxSize().padding(SheetPadding)) {
        SheetHeading(overline = "Сортировка", title = "Сначала показывать")
        Column(Modifier.offset(x = -TvTextRowInset).padding(top = 14.dp).tvFocusGroup(fallback = current)) {
            SortOptions.forEach { (field, label) ->
                val selected = field == sort.field
                TvTextRow(
                    text = label,
                    selected = selected,
                    onClick = { dispatch(SearchEvent.SortChange(sort.copy(field = field))) },
                    modifier = if (selected) Modifier.focusRequester(current) else Modifier,
                )
            }
        }
        TvOverline("Порядок", Modifier.padding(top = SectionGap, bottom = 10.dp), accent = false)
        Row(horizontalArrangement = Arrangement.spacedBy(ChipGap)) {
            TvChip(
                label = "По убыванию",
                selected = !sort.ascending,
                onClick = { dispatch(SearchEvent.SortChange(sort.copy(ascending = false))) },
            )
            TvChip(
                label = "По возрастанию",
                selected = sort.ascending,
                onClick = { dispatch(SearchEvent.SortChange(sort.copy(ascending = true))) },
            )
        }
    }
}

/**
 * Фильтры: год, оценки, страна и переключатели — всё чипами: на пульте слайдер неуправляем, а
 * перебор чипов «ОК»-ом естественен. Правки — черновик в модели; выдачу меняет только «Применить».
 */
@Composable
private fun FiltersSheet(draft: CatalogFilters, countries: List<Country>, dispatch: (SearchEvent) -> Unit) {
    val edit: (CatalogFilters) -> Unit = { dispatch(SearchEvent.EditFilters(it)) }
    val firstChip = remember { FocusRequester() }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SheetPadding)
            .tvFocusGroup(fallback = firstChip),
        verticalArrangement = Arrangement.spacedBy(SectionGap),
    ) {
        SheetHeading(overline = "Каталог", title = "Фильтры")
        YearSection(draft, edit, Modifier.focusRequester(firstChip))
        RatingSection("Рейтинг КиноПоиска", draft.kpRatingFrom) { edit(draft.copy(kpRatingFrom = it)) }
        RatingSection("Рейтинг IMDb", draft.imdbRatingFrom) { edit(draft.copy(imdbRatingFrom = it)) }
        CountrySection(countries, draft.countryId) { edit(draft.copy(countryId = it)) }
        FilterSection("Качество и статус") {
            TvChip(label = "Только 4K", selected = draft.only4k, onClick = { edit(draft.copy(only4k = !draft.only4k)) })
            // Тумблер: включён — только завершённые, выключен — любые (null). «Только идущие» домен
            // допускает, но на пульте третье состояние у одного чипа не прочитать.
            TvChip(
                label = "Только завершённые",
                selected = draft.onlyFinished == true,
                onClick = { edit(draft.copy(onlyFinished = if (draft.onlyFinished == true) null else true)) },
            )
        }
        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TvButton(text = "Применить", onClick = { dispatch(SearchEvent.ApplyDraftFilters) })
            TvButton(
                text = "Сбросить",
                onClick = { dispatch(SearchEvent.ResetDraftFilters) },
                style = TvButtonStyle.Secondary
            )
        }
    }
}

@Composable
private fun SheetHeading(overline: String, title: String) {
    TvOverline(overline)
    Text(title, style = SheetTitle, color = LocalFxColors.current.text, modifier = Modifier.padding(top = 8.dp))
}

/** Заголовок секции над чипами с переносом: искать скрытые за краем чипы с пульта неудобно. */
@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TvOverline(title, accent = false)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(ChipGap),
            verticalArrangement = Arrangement.spacedBy(ChipGap)
        ) {
            content()
        }
    }
}

@Composable
private fun YearSection(draft: CatalogFilters, edit: (CatalogFilters) -> Unit, firstChipModifier: Modifier) {
    FilterSection("Год выпуска") {
        YearBuckets.forEachIndexed { index, bucket ->
            TvChip(
                label = bucket.label,
                selected = bucket.matches(draft),
                onClick = { edit(draft.copy(yearFrom = bucket.yearFrom, yearTo = bucket.yearTo)) },
                style = TvChipStyle.Outlined,
                modifier = if (index == 0) firstChipModifier else Modifier,
            )
        }
    }
}

@Composable
private fun RatingSection(title: String, selected: Int?, onSelect: (Int?) -> Unit) {
    FilterSection(title) {
        RatingThresholds.forEach { (label, threshold) ->
            TvChip(
                label = label,
                selected = selected == threshold,
                onClick = { onSelect(threshold) },
                style = TvChipStyle.Outlined,
            )
        }
    }
}

/** Стран десятки — горизонтальным списком, первый вход фокуса — на «Любая». */
@Composable
private fun CountrySection(countries: List<Country>, selectedId: Int?, onSelect: (Int?) -> Unit) {
    val anyCountry = remember { FocusRequester() }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TvOverline("Страна", accent = false)
        // Сдвиг на запас под фокус: увеличенный крайний чип не срезается краем списка.
        LazyRow(
            modifier = Modifier.offset(x = -TvMetrics.FocusInset).tvFocusGroup(fallback = anyCountry),
            contentPadding = PaddingValues(horizontal = TvMetrics.FocusInset),
            horizontalArrangement = Arrangement.spacedBy(ChipGap),
        ) {
            item(key = "any") {
                TvChip(
                    label = "Любая",
                    selected = selectedId == null,
                    onClick = { onSelect(null) },
                    style = TvChipStyle.Outlined,
                    modifier = Modifier.focusRequester(anyCountry),
                )
            }
            items(countries, key = { it.id }) { country ->
                TvChip(
                    label = country.title,
                    selected = country.id == selectedId,
                    onClick = { onSelect(country.id) },
                    style = TvChipStyle.Outlined,
                )
            }
        }
    }
}

/** Десятилетие как границы года. Пустая граница — «не ограничивать» с этой стороны. */
private data class YearBucket(val label: String, val yearFrom: Int?, val yearTo: Int?) {
    /** Выбран тот чип, чьи границы совпадают с черновиком, — иначе выбор не читался бы. */
    fun matches(filters: CatalogFilters): Boolean = filters.yearFrom == yearFrom && filters.yearTo == yearTo
}

private val YearBuckets = listOf(
    YearBucket(label = "Любой", yearFrom = null, yearTo = null),
    YearBucket(label = "2020-е", yearFrom = 2020, yearTo = 2029),
    YearBucket(label = "2010-е", yearFrom = 2010, yearTo = 2019),
    YearBucket(label = "2000-е", yearFrom = 2000, yearTo = 2009),
    YearBucket(label = "1990-е", yearFrom = 1990, yearTo = 1999),
    YearBucket(label = "1980-е", yearFrom = 1980, yearTo = 1989),
    YearBucket(label = "до 1980", yearFrom = null, yearTo = 1979),
)

/** Пороги внешних оценок (kino.pub 0–10). «Любой» — без нижней границы. */
private val RatingThresholds: List<Pair<String, Int?>> = listOf(
    "Любой" to null,
    "5+" to 5,
    "6+" to 6,
    "7+" to 7,
    "8+" to 8,
    "9+" to 9,
)

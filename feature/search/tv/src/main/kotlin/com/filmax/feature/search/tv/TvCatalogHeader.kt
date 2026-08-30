// Шапка каталога: строка поиска сверху, под ней чипы типа/сортировки и жанров, а снизу
// сводка выборки. Сетка постеров и состояние экрана — в TvCatalogScreen.
package com.filmax.feature.search.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.tv.designsystem.TvChip
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOnSurfaceDim
import com.filmax.core.ui.components.typeLabel
import com.filmax.feature.search.common.SearchState
import com.filmax.feature.search.common.SortOptions
import com.filmax.feature.search.common.TypeOptions
import com.filmax.feature.search.common.sortLabel

@Composable
internal fun CatalogHeader(
    state: SearchState,
    searchModifier: Modifier,
    actions: CatalogActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        CatalogSearchBar(
            query = state.query,
            onQuery = actions.onQuery,
            onVoice = actions.onVoice,
            onEditingFinished = actions.onEditingFinished,
            modifier = searchModifier,
        )
        Spacer(Modifier.height(16.dp))
        // Явная связь «вниз»: ряд типов → первый жанр. Спатиальный поиск здесь ненадёжен:
        // с focusRestorer на обоих рядах DOWN проскакивал жанры и падал сразу в сетку постеров.
        val firstGenreFocus = remember { FocusRequester() }
        val hasGenres = state.genres.isNotEmpty()
        CatalogTypeRow(
            state = state,
            actions = actions,
            downFocus = firstGenreFocus.takeIf { hasGenres },
        )
        if (hasGenres) {
            Spacer(Modifier.height(12.dp))
            CatalogGenreRow(
                genres = state.genres,
                selectedId = state.selectedGenreId,
                onGenre = actions.onGenre,
                firstChipFocus = firstGenreFocus,
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = catalogSummary(state),
            style = MaterialTheme.typography.bodySmall,
            color = TvOnSurfaceDim,
            modifier = Modifier.padding(horizontal = TvMetrics.SafeHorizontal),
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CatalogTypeRow(
    state: SearchState,
    actions: CatalogActions,
    downFocus: FocusRequester?,
) {
    val sort = state.sort
    val filters = state.filters
    var filtersOpen by remember { mutableStateOf(false) }
    // Первый вход фокуса в ряд — всегда на первый чип (fallback focusRestorer): без него D-pad
    // сажал фокус на пространственно-ближайший чип в середине ряда (строка поиска сверху и сетка
    // снизу — во всю ширину). Повторные входы восстанавливают последний сфокусированный.
    val firstTypeChipFocus = remember { FocusRequester() }
    // «Вниз» с любого чипа — на первый жанр. Свойство стоит на КАЖДОМ чипе: focusProperties
    // контейнера на детей не распространяется, и спатиальный поиск скипал ряд жанров в сетку.
    val chipModifier = Modifier.focusProperties { downFocus?.let { down = it } }
    // Горизонтальный скролл, а не Row: тип + сортировка + «Фильтры» не влезали в safe area, и
    // последний чип клипился. Разделители-палочки убраны — от них между группами зиял большой
    // отступ; теперь шаг между всеми чипами одинаковый. offset/contentPadding — как у ряда жанров.
    LazyRow(
        modifier = Modifier.fillMaxWidth().focusRestorer(firstTypeChipFocus),
        contentPadding = PaddingValues(horizontal = TvMetrics.SafeHorizontal),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(TypeOptions) { index, (type, label) ->
            TvChip(
                label = label,
                selected = state.filter == type,
                onClick = { actions.onFilter(type) },
                modifier = if (index == 0) chipModifier.focusRequester(firstTypeChipFocus) else chipModifier,
            )
        }
        // Поле сортировки: OK листает по кругу. Стрелка ↕ (U+2195), а не ⇅ из макета: у второй
        // покрытие во встроенных шрифтах Android TV не гарантировано.
        item {
            TvChip(
                label = "↕ ${sortLabel(sort.field)}",
                selected = false,
                onClick = { actions.onSort(SortOption(nextSort(sort.field), sort.ascending)) },
                modifier = chipModifier,
            )
        }
        // Направление: ↑ по возрастанию (kino.pub `-field`), ↓ по убыванию.
        item {
            TvChip(
                label = if (sort.ascending) "↑ Возр." else "↓ Убыв.",
                selected = false,
                onClick = { actions.onSort(sort.copy(ascending = !sort.ascending)) },
                modifier = chipModifier,
            )
        }
        // Полный набор фильтров (год, рейтинги, страна, 4K, завершённость) — в оверлей-панели.
        item {
            TvChip(
                label = if (filters.activeCount > 0) "Фильтры · ${filters.activeCount}" else "Фильтры",
                selected = filters.activeCount > 0,
                onClick = { filtersOpen = true },
                modifier = chipModifier,
            )
        }
    }
    if (filtersOpen) {
        TvCatalogFilterDialog(
            current = filters,
            countries = state.countries,
            onApply = { actions.onApplyFilters(it) },
            onDismiss = { filtersOpen = false },
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CatalogGenreRow(
    genres: List<Genre>,
    selectedId: Int?,
    onGenre: (Int?) -> Unit,
    /** Привязывается к первому жанру: fallback focusRestorer и цель `down` ряда типов. */
    firstChipFocus: FocusRequester,
) {
    LazyRow(
        // От края до края: viewport на всю ширину, а первый/последний чип держит на линии safe area
        // contentPadding. Так чипы скроллятся к самым краям экрана, а не обрываются на safe-границе.
        modifier = Modifier.fillMaxWidth().focusRestorer(firstChipFocus),
        contentPadding = PaddingValues(horizontal = TvMetrics.SafeHorizontal),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(genres, key = { _, genre -> genre.id }) { index, genre ->
            TvChip(
                label = genre.title,
                selected = genre.id == selectedId,
                // Повторный OK по выбранному жанру снимает фильтр — отдельного чипа «Все» в ряду нет.
                onClick = { onGenre(if (genre.id == selectedId) null else genre.id) },
                modifier = if (index == 0) Modifier.focusRequester(firstChipFocus) else Modifier,
            )
        }
    }
}

/** Описание текущей выборки: `Фильмы · Драма · 24 результата` (макет: catFilterLabel). */
private fun catalogSummary(state: SearchState): String {
    val parts = buildList {
        add(typeLabel(state.filter))
        state.genres.firstOrNull { it.id == state.selectedGenreId }?.let { add(it.title) }
        add(resultsCount(state.visibleItems.size))
    }
    return parts.joinToString(" · ")
}

/** Подпись чипа-фильтра: множественное число. */
private fun typeLabel(type: ItemType?): String =
    TypeOptions.firstOrNull { it.first == type }?.second ?: TypeOptions.first().second

private fun nextSort(current: CatalogSort): CatalogSort {
    val index = SortOptions.indexOfFirst { it.first == current }
    return SortOptions[(index + 1) % SortOptions.size].first
}

/** «24 результата» — с русским числительным, иначе строка читается как машинный лог. */
private fun resultsCount(count: Int): String {
    val word = when {
        count % HUNDRED in TEENS -> "результатов"
        count % TEN == 1 -> "результат"
        count % TEN in FEW -> "результата"
        else -> "результатов"
    }
    return "$count $word"
}

private const val TEN = 10
private const val HUNDRED = 100
private val TEENS = 11..14
private val FEW = 2..4

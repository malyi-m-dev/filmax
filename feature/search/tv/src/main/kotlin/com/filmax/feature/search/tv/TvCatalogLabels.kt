// Подписи ТВ-каталога и поиска — чистые функции без UI: сводка выборки со счётчиком, подписи
// чипов, «Ещё N результатов», карточка лучшего совпадения с подсветкой запроса.
package com.filmax.feature.search.tv

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.tv.designsystem.TvSpotlight
import com.filmax.core.presentation.labels.isHighRating
import com.filmax.core.presentation.labels.posterMeta
import com.filmax.core.presentation.labels.ratingLabel
import com.filmax.core.presentation.labels.typeLabel
import com.filmax.feature.search.common.CatalogCount
import com.filmax.feature.search.common.MIN_QUERY_LENGTH
import com.filmax.feature.search.common.SearchState
import com.filmax.feature.search.common.TypeOptions
import com.filmax.feature.search.common.matchRange
import com.filmax.feature.search.common.sortLabel

/**
 * Сводка над сеткой: «Фантастика · 42 результата», «Сериалы · около 60 результатов». Чип «Все»
 * в сводку не попадает — он ничего не сужает. Пока витрина грузится, счётчика нет.
 */
internal fun catalogSummary(state: SearchState): String = listOfNotNull(
    TypeOptions.firstOrNull { (type, _) -> type == state.filter && type != null }?.second,
    state.genres.firstOrNull { it.id == state.selectedGenreId }?.title,
    state.catalogCount?.let(::countLabel),
).joinToString(" · ")

/**
 * Счётчик выдачи. Точный — «42 результата»; оценка по числу страниц — «около 62 результатов»:
 * kino.pub отдаёт страницы, а не тайтлы, и до последней страницы честнее сказать «около».
 */
internal fun countLabel(count: CatalogCount): String {
    val value = count.value
    return if (count.exact) "$value ${resultsWord(value)}" else "около $value ${resultsAfterAbout(value)}"
}

/** «Ещё 6 результатов» — над постерами под лучшим совпадением. */
internal fun moreResultsLabel(count: Int): String = "Ещё $count ${resultsWord(count)}"

/** Чип сортировки: поле и стрелка, если порядок обратный привычному («Год ↑»). */
internal fun sortChipLabel(sort: SortOption): String =
    if (sort.ascending) "${sortLabel(sort.field)} ↑" else sortLabel(sort.field)

/** Чип фильтров: «Фильтры» или «Фильтры · 2» — сколько применено. */
internal fun filtersChipLabel(filters: CatalogFilters): String =
    if (filters.activeCount > 0) "Фильтры · ${filters.activeCount}" else "Фильтры"

/** Название с подсветкой совпадения с [query] цветом [highlight]; нет совпадения — как есть. */
internal fun highlightedTitle(title: String, query: String, highlight: Color): AnnotatedString {
    val match = matchRange(title, query) ?: return AnnotatedString(title)
    return buildAnnotatedString {
        append(title.substring(0, match.first))
        withStyle(SpanStyle(color = highlight)) { append(title.substring(match)) }
        append(title.substring(match.last + 1))
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

/** Что сказать, пока карточек нет: набрать запрос, подождать или — что ничего не нашлось. */
internal fun searchHint(state: SearchState): String = when {
    state.query.trim().length < MIN_QUERY_LENGTH -> "Наберите название — результаты появятся здесь"
    state.loading -> "Ищем…"
    else -> "По запросу «${state.query.trim()}» ничего не нашлось"
}

/** «1 результат», «2 результата», «5 результатов». */
private fun resultsWord(count: Int): String = when {
    count % HUNDRED in TEENS -> "результатов"
    count % TEN == 1 -> "результат"
    count % TEN in FEW -> "результата"
    else -> "результатов"
}

/** После «около» — родительный падеж: «около 21 результата», «около 62 результатов». */
private fun resultsAfterAbout(count: Int): String =
    if (count % TEN == 1 && count % HUNDRED !in TEENS) "результата" else "результатов"

private const val TEN = 10
private const val HUNDRED = 100
private val TEENS = 11..14
private val FEW = 2..4

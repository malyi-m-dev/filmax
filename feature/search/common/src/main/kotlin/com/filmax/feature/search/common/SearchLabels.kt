// Подписи каталога и поиска, общие для телефона и ТВ: сводка выборки со счётчиком, чипы сортировки
// и фильтров, применённые фильтры по одному, «Ещё N результатов», подсказка пустого поиска.
// Чистые функции: вёрстка их только зовёт, тесты проверяют без Robolectric.

package com.filmax.feature.search.common

import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.Country

/**
 * Сводка над сеткой: «Фантастика · 42 результата», «Сериалы · около 60 результатов». Чип «Все»
 * в сводку не попадает — он ничего не сужает. Пока витрина грузится, счётчика нет.
 */
fun catalogSummary(state: SearchState): String = listOfNotNull(
    TypeOptions.firstOrNull { (type, _) -> type == state.filter && type != null }?.second,
    state.genres.firstOrNull { it.id == state.selectedGenreId }?.title,
    state.catalogCount?.let(::countLabel),
).joinToString(" · ")

/**
 * Счётчик выдачи. Точный — «42 результата»; оценка по числу страниц — «около 62 результатов»:
 * kino.pub отдаёт страницы, а не тайтлы, и до последней страницы честнее сказать «около».
 */
fun countLabel(count: CatalogCount): String {
    val value = count.value
    return if (count.exact) "$value ${resultsWord(value)}" else "около $value ${resultsAfterAbout(value)}"
}

/** «Ещё 6 результатов» — над постерами под лучшим совпадением. */
fun moreResultsLabel(count: Int): String = "Ещё $count ${resultsWord(count)}"

/** Чип сортировки: поле и стрелка, если порядок обратный привычному («Год ↑»). */
fun sortChipLabel(sort: SortOption): String =
    if (sort.ascending) "${sortLabel(sort.field)} ↑" else sortLabel(sort.field)

/** Чип фильтров: «Фильтры» или «Фильтры · 2» — сколько применено. */
fun filtersChipLabel(filters: CatalogFilters): String =
    if (filters.activeCount > 0) "Фильтры · ${filters.activeCount}" else "Фильтры"

/** Что сказать, пока карточек нет: набрать запрос, подождать или — что ничего не нашлось. */
fun searchHint(state: SearchState): String = when {
    state.query.trim().length < MIN_QUERY_LENGTH -> "Наберите название — результаты появятся здесь"
    state.loading -> "Ищем…"
    else -> "По запросу «${state.query.trim()}» ничего не нашлось"
}

/** Применённый фильтр чипом над сеткой: подпись и фильтры без него — то, что оставит «✕». */
data class AppliedFilter(val label: String, val without: CatalogFilters)

/**
 * Применённые фильтры по одному: каждый — свой чип, и «✕» снимает только его. Порядок — как в
 * шторке фильтров: год, оценки, страна, 4K, завершённость.
 */
fun appliedFilters(filters: CatalogFilters, countries: List<Country>): List<AppliedFilter> = buildList {
    yearLabel(filters)?.let { add(AppliedFilter(it, filters.copy(yearFrom = null, yearTo = null))) }
    filters.kpRatingFrom?.let { add(AppliedFilter("КП $it+", filters.copy(kpRatingFrom = null))) }
    filters.imdbRatingFrom?.let { add(AppliedFilter("IMDb $it+", filters.copy(imdbRatingFrom = null))) }
    filters.countryId?.let { id ->
        val country = countries.firstOrNull { it.id == id }?.title ?: "Страна"
        add(AppliedFilter(country, filters.copy(countryId = null)))
    }
    if (filters.only4k) add(AppliedFilter("4K", filters.copy(only4k = false)))
    finishedLabel(filters)?.let { add(AppliedFilter(it, filters.copy(onlyFinished = null))) }
}

private fun yearLabel(filters: CatalogFilters): String? {
    val from = filters.yearFrom
    val to = filters.yearTo
    return when {
        from != null && to != null -> if (from == to) "$from" else "$from–$to"
        from != null -> "С $from"
        to != null -> "До $to"
        else -> null
    }
}

private fun finishedLabel(filters: CatalogFilters): String? = when (filters.onlyFinished) {
    true -> "Завершённые"
    false -> "Продолжаются"
    null -> null
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

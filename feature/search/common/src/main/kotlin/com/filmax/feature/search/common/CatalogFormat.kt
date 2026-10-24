// Общие для mobile и tv справочники каталога: чипы типов, поля сортировки и русские подписи.
// Чистые данные без UI-зависимостей.

package com.filmax.feature.search.common

import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.model.Country
import com.filmax.core.domain.catalog.model.ItemType

/** Типы в порядке чипов. null — «Все» (объединение типов, см. SearchScreenModel). */
val TypeOptions = listOf(
    null to "Все",
    ItemType.MOVIE to "Фильмы",
    ItemType.SERIES to "Сериалы",
    ItemType.ANIME to "Аниме",
    ItemType.DOCUMENTARY to "Документальные",
)

/** Поля сортировки в порядке меню/перебора чипа. Русские подписи — из макета. */
val SortOptions = listOf(
    CatalogSort.UPDATED to "Обновлённые",
    CatalogSort.CREATED to "Добавленные",
    CatalogSort.RATING to "Рейтинг Filmax",
    CatalogSort.VIEWS to "Просмотры",
    CatalogSort.YEAR to "Год",
    CatalogSort.KINOPOISK_RATING to "Рейтинг КП",
    CatalogSort.IMDB_RATING to "Рейтинг IMDb",
)

fun sortLabel(sort: CatalogSort): String =
    SortOptions.firstOrNull { it.first == sort }?.second ?: SortOptions.first().second

/** Применённый фильтр чипом над сеткой: подпись и фильтры без него — то, что оставит «✕». */
data class AppliedFilter(val label: String, val without: CatalogFilters)

/**
 * Применённые фильтры по одному: каждый — свой чип, и «✕» снимает только его. Порядок — как в
 * шторке фильтров: год, оценки, страна, 4K, завершённость.
 */
fun CatalogFilters.applied(countries: List<Country>): List<AppliedFilter> = buildList {
    yearLabel()?.let { add(AppliedFilter(it, copy(yearFrom = null, yearTo = null))) }
    kpRatingFrom?.let { add(AppliedFilter("КП $it+", copy(kpRatingFrom = null))) }
    imdbRatingFrom?.let { add(AppliedFilter("IMDb $it+", copy(imdbRatingFrom = null))) }
    countryId?.let { id ->
        add(AppliedFilter(countries.firstOrNull { it.id == id }?.title ?: "Страна", copy(countryId = null)))
    }
    if (only4k) add(AppliedFilter("4K", copy(only4k = false)))
    finishedLabel()?.let { add(AppliedFilter(it, copy(onlyFinished = null))) }
}

private fun CatalogFilters.yearLabel(): String? {
    val from = yearFrom
    val to = yearTo
    return when {
        from != null && to != null -> if (from == to) "$from" else "$from–$to"
        from != null -> "С $from"
        to != null -> "До $to"
        else -> null
    }
}

private fun CatalogFilters.finishedLabel(): String? = when (onlyFinished) {
    true -> "Завершённые"
    false -> "Продолжаются"
    null -> null
}

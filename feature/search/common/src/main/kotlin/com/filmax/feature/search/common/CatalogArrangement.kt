// Состав и порядок выдачи на клиенте: фильтры экрана поверх результатов поиска, досортировка по
// полям карточки и склейка догруженной страницы без дублей ключей сетки.

package com.filmax.feature.search.common

import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.Item

/**
 * Жанр, диапазонные фильтры и сортировка поверх выдачи поиска: сам `search` ничего из этого не
 * принимает. Страну (по id) и 4K локально не проверить — в [Item] их нет, они остаются серверными
 * и на результаты поиска не влияют. На ТВ поиск — отдельный режим без чипов каталога: фильтр,
 * которого зритель не видит, тихо прятал бы найденное, поэтому там выдача не фильтруется.
 */
internal fun arrangedResults(state: SearchState, found: List<Item>): List<Item> {
    // distinctBy: поиск умеет вернуть один id дважды — в сетке это дубль ключа и краш.
    if (state.searchOpen) return found.distinctBy { it.id }
    val genreId = state.selectedGenreId
    val filtered = found.asSequence()
        .filter { item -> genreId == null || item.genres.any { it.id == genreId } }
        .filter { item -> matchesFilters(item, state.filters) }
        .distinctBy { it.id }
        .toList()
    return sortedLocally(filtered, state.sort)
}

/**
 * Догруженная страница в хвост витрины. filter гасит id, уже стоящие в сетке; distinctBy —
 * дубли внутри самой страницы (склейка типов): две карточки с одним id роняют LazyGrid по
 * неуникальному ключу.
 */
internal fun appendedPage(shown: List<Item>, page: List<Item>, sort: SortOption): List<Item> {
    val seen = shown.mapTo(HashSet()) { it.id }
    return sortedLocally((shown + page.filter { it.id !in seen }).distinctBy { it.id }, sort)
}

/** Локальная проверка фильтров по полям, которые есть в [Item] (год, рейтинги, завершённость). */
internal fun matchesFilters(item: Item, filters: CatalogFilters): Boolean {
    // Локальные копии: nullable-поля CatalogFilters лежат в другом модуле, и смарт-каст через
    // границу модуля невозможен — сравнивать надо через захваченное значение.
    val yearFrom = filters.yearFrom
    val yearTo = filters.yearTo
    val kpFrom = filters.kpRatingFrom
    val imdbFrom = filters.imdbRatingFrom
    val finishedFilter = filters.onlyFinished
    return (yearFrom == null || item.year >= yearFrom) &&
        (yearTo == null || item.year <= yearTo) &&
        (kpFrom == null || ratingAtLeast(item.rating.kinopoisk, kpFrom)) &&
        (imdbFrom == null || ratingAtLeast(item.rating.imdb, imdbFrom)) &&
        (finishedFilter == null || item.finished == finishedFilter)
}

private fun ratingAtLeast(raw: String?, threshold: Int): Boolean {
    val value = raw?.toDoubleOrNull() ?: return false
    return value >= threshold
}

/**
 * Досортировка на клиенте. Набор карточек выбирает сервер, но по «Рейтингу» и «Году» он
 * сортирует по своим полям, а карточка показывает НАШ усреднённый рейтинг (IMDb+КП) и год —
 * без локального прохода первой в сетке стояла бы не та карточка, которую зритель видит лучшей.
 * У остальных ключей (просмотры, новизна, рейтинги КП/IMDb по отдельности) локального поля в
 * [Item] нет: там доверяем порядку сервера как есть, направление он тоже уже применил.
 */
internal fun sortedLocally(items: List<Item>, sort: SortOption): List<Item> {
    val comparator = when (sort.field) {
        CatalogSort.RATING -> compareBy<Item> { it.rating.external }
        CatalogSort.YEAR -> compareBy<Item> { it.year }
        else -> return items
    }
    return if (sort.ascending) items.sortedWith(comparator) else items.sortedWith(comparator.reversed())
}

package com.filmax.core.domain.catalog

import com.filmax.core.domain.catalog.model.Item

/**
 * Локальная обработка выдачи — общая для Android и iOS. Набор тайтлов выбирает источник, но:
 * - поиск не принимает ни жанра, ни фильтров — их применяем сами по полям [Item];
 * - страницы пересекаются, а сетка с повторным id падает (id — ключ карточки);
 * - по «Рейтингу» и «Году» источник сортирует по своим полям, а карточка показывает усреднённую
 *   оценку (`rating.external`) и год — без локального прохода первой стояла бы не та карточка.
 *
 * Пороги оценок — только по тем, что задал фильтр: порог по оценке, которой у источника нет,
 * экран не предлагает (`SourceCapabilities.ratings`).
 */
object ArrangeCatalog {

    /** Выдача поиска с жанром, фильтрами и сортировкой экрана. Страну и 4K в [Item] не проверить. */
    fun searchResults(items: List<Item>, genreId: Int?, filters: CatalogFilters, sort: SortOption): List<Item> {
        val filtered = items.asSequence()
            .filter { item -> genreId == null || item.genres.any { it.id == genreId } }
            .filter { item -> item.matches(filters) }
            .distinctBy { it.id }
            .toList()
        return sorted(filtered, sort)
    }

    /** Первая страница витрины: фильтры уже применил источник. */
    fun firstPage(items: List<Item>, sort: SortOption): List<Item> = sorted(items.distinctBy { it.id }, sort)

    /** Следующая страница в хвост показанной: уже стоящие в сетке id гасятся. */
    fun nextPage(shown: List<Item>, page: List<Item>, sort: SortOption): List<Item> {
        val seen = shown.mapTo(HashSet()) { it.id }
        return sorted((shown + page.filter { it.id !in seen }).distinctBy { it.id }, sort)
    }

    /** Фильтры по полям, которые есть в [Item]: год, оценки, завершённость. */
    fun Item.matches(filters: CatalogFilters): Boolean {
        // Локальные копии: nullable-поля CatalogFilters смарт-каст не берёт.
        val yearFrom = filters.yearFrom
        val yearTo = filters.yearTo
        val finishedFilter = filters.onlyFinished
        return (yearFrom == null || year >= yearFrom) &&
            (yearTo == null || year <= yearTo) &&
            filters.minRatings.all { (source, threshold) -> (rating.of(source) ?: 0.0) >= threshold } &&
            (finishedFilter == null || finished == finishedFilter)
    }

    /**
     * Досортировка по ключам, у которых есть локальное поле. У остальных (просмотры, новизна,
     * отдельные оценки) доверяем порядку источника: направление он тоже уже применил.
     */
    private fun sorted(items: List<Item>, sort: SortOption): List<Item> {
        val comparator = when (sort.field) {
            CatalogSort.RATING -> compareBy<Item> { it.rating.external }
            CatalogSort.YEAR -> compareBy<Item> { it.year }
            else -> return items
        }
        return if (sort.ascending) items.sortedWith(comparator) else items.sortedWith(comparator.reversed())
    }
}

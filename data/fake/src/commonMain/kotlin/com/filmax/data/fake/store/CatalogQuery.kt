package com.filmax.data.fake.store

import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.ItemType

/**
 * Запрос к выдаче каталога — то, что настоящий репозиторий раскладывает в параметры
 * `api/v1/items`. Короткие перегрузки репозитория сводятся к нему же: тип плюс поле сортировки
 * по убыванию.
 */
internal data class CatalogQuery(
    val type: ItemType,
    val genreId: Int? = null,
    val filters: CatalogFilters = CatalogFilters(),
    val sort: SortOption = SortOption(CatalogSort.UPDATED),
)

/** Вес года в ключе «Обновлённые»: год старше любого id, id лишь разводит тайтлы одного года. */
private const val UPDATED_YEAR_WEIGHT = 100_000.0

internal fun CatalogEntry.matches(query: CatalogQuery): Boolean =
    isOfType(query.type) && hasGenre(query.genreId) && matches(query.filters)

/**
 * Сортировка с направлением. Тайтлы без значения поля (нет оценки КП или IMDb) уходят в хвост
 * при любом направлении: и «лучшее сверху», и «худшее сверху» начинаются с реальных оценок.
 */
internal fun List<CatalogEntry>.arrangedBy(sort: SortOption): List<CatalogEntry> {
    val (valued, unvalued) = partition { it.sortValue(sort.field) != null }
    val ordered = if (sort.ascending) {
        valued.sortedBy { it.sortValue(sort.field) }
    } else {
        valued.sortedByDescending { it.sortValue(sort.field) }
    }
    return ordered + unvalued
}

private fun CatalogEntry.hasGenre(genreId: Int?): Boolean =
    genreId == null || item.genres.any { it.id == genreId }

/** Фильтры так, как их понимает сервер: незаданное поле не фильтрует. */
private fun CatalogEntry.matches(filters: CatalogFilters): Boolean = listOf(
    item.year.isAtLeast(filters.yearFrom),
    item.year.isAtMost(filters.yearTo),
    item.rating.kinopoisk.reaches(filters.kpRatingFrom),
    item.rating.imdb.reaches(filters.imdbRatingFrom),
    filters.countryId == null || filters.countryId == countryId,
    !filters.only4k || has4k,
    filters.onlyFinished == null || filters.onlyFinished == item.finished,
).all { it }

private fun Int.isAtLeast(bound: Int?): Boolean = bound == null || this >= bound

private fun Int.isAtMost(bound: Int?): Boolean = bound == null || this <= bound

/** Порог рейтинга: тайтл без оценки порог не проходит — сервер такие отсекает. */
private fun String?.reaches(threshold: Int?): Boolean =
    threshold == null || this?.toDoubleOrNull()?.let { score -> score >= threshold } == true

/**
 * Значение поля сортировки; null — у тайтла его нет. «Обновлённые» на живом API — дата последней
 * серии; у фикстур такой даты нет, и ближе всего к ней свежесть года выпуска.
 */
private fun CatalogEntry.sortValue(field: CatalogSort): Double? = when (field) {
    CatalogSort.UPDATED -> item.year * UPDATED_YEAR_WEIGHT + id
    CatalogSort.CREATED -> id.toDouble()
    CatalogSort.RATING -> item.rating.filmax.takeIf { it > 0 }?.toDouble()
    CatalogSort.VIEWS -> views.toDouble()
    CatalogSort.YEAR -> item.year.toDouble()
    CatalogSort.KINOPOISK_RATING -> item.rating.kinopoisk?.toDoubleOrNull()
    CatalogSort.IMDB_RATING -> item.rating.imdb?.toDoubleOrNull()
}

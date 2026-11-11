package com.filmax.shared

import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.CatalogQuery
import com.filmax.core.domain.catalog.GenreFilter
import com.filmax.core.domain.catalog.ReferenceGenre
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.RatingSource
import com.filmax.core.domain.source.ContentSource
import com.filmax.core.domain.source.SourceCapabilities
import com.filmax.core.domain.source.capabilities

/**
 * Каталог для Swift. Значения по умолчанию Kotlin в Swift не переносятся, а сборка запроса витрины
 * должна совпадать с Android (SearchReducers): «Все» — фильмы, сериалы и документальное, «Аниме» —
 * жанр поверх фильмов и сериалов.
 */
object CatalogKit {

    /** Фильтры без условий. */
    val noFilters: CatalogFilters = CatalogFilters()

    /** Что умеет источник: оценки, сортировки, страны, папки, подписка. */
    fun capabilities(source: ContentSource): SourceCapabilities = source.capabilities

    /** Запрос витрины по чипу типа ([type] null — «Все»), жанру из ряда и фильтрам. */
    fun query(type: ItemType?, genreId: Int?, filters: CatalogFilters, sort: SortOption): CatalogQuery =
        CatalogQuery(
            types = when (type) {
                null -> listOf(ItemType.MOVIE, ItemType.SERIES, ItemType.DOCUMENTARY)
                ItemType.ANIME -> listOf(ItemType.MOVIE, ItemType.SERIES)
                else -> listOf(type)
            },
            genre = if (type == ItemType.ANIME) {
                GenreFilter.Reference(ReferenceGenre.ANIME)
            } else {
                genreId?.let(GenreFilter::Listed)
            },
            filters = filters,
            sort = sort,
        )

    /** Годы «с — по»; null — без границы. */
    fun withYears(filters: CatalogFilters, from: Int?, to: Int?): CatalogFilters =
        filters.copy(yearFrom = from, yearTo = to)

    /** Порог оценки [source]; null — снять. */
    fun withMinRating(filters: CatalogFilters, source: RatingSource, value: Int?): CatalogFilters =
        filters.withMinRating(source, value)

    fun minRating(filters: CatalogFilters, source: RatingSource): Int? = filters.minRating(source)

    fun withCountry(filters: CatalogFilters, countryId: Int?): CatalogFilters = filters.copy(countryId = countryId)

    fun withOnly4k(filters: CatalogFilters, only4k: Boolean): CatalogFilters = filters.copy(only4k = only4k)

    /** Только завершённые сериалы: true — завершённые, false — идущие, null — все. */
    fun withFinished(filters: CatalogFilters, finished: Boolean?): CatalogFilters =
        filters.copy(onlyFinished = finished)
}

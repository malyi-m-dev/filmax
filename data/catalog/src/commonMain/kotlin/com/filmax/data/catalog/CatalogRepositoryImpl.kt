package com.filmax.data.catalog

import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.CollectionPage
import com.filmax.core.domain.catalog.model.Country
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemPage
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.common.safeRequest
import com.filmax.data.catalog.mapper.toDomain
import com.filmax.data.catalog.remote.CatalogApi
import com.filmax.data.catalog.remote.ItemsQuery

// Значение параметра `quality` для фильтра «только 4K» (kino.pub: 4 = 2160p).
private const val QUALITY_4K = 4

// Реализация всего контракта CatalogRepository — столько же методов, дробить незачем.
@Suppress("TooManyFunctions")
internal class CatalogRepositoryImpl(
    private val api: CatalogApi,
) : CatalogRepository {

    override suspend fun getItems(type: ItemType, sort: CatalogSort, page: Int): RequestResult<ItemPage> =
        safeRequest { api.getItems(type.apiValue, sort.descending, page).toDomain() }

    override suspend fun getItemsByGenre(
        type: ItemType,
        genreId: Int,
        sort: CatalogSort,
        page: Int,
    ): RequestResult<ItemPage> =
        safeRequest { api.getItemsByGenre(type.apiValue, genreId, sort.descending, page).toDomain() }

    override suspend fun getItems(
        type: ItemType,
        genreId: Int?,
        filters: CatalogFilters,
        sort: SortOption,
        page: Int,
    ): RequestResult<ItemPage> =
        safeRequest { api.getFilteredItems(filters.toQuery(type, genreId, sort, page)).toDomain() }

    override suspend fun getHotItems(type: ItemType, page: Int): RequestResult<ItemPage> =
        safeRequest { api.getItemsByShortcut("hot", type.apiValue, page).toDomain() }

    override suspend fun getNewItems(type: ItemType, page: Int): RequestResult<ItemPage> =
        safeRequest { api.getItemsByShortcut("new", type.apiValue, page).toDomain() }

    override suspend fun getItemDetails(id: Int): RequestResult<Item> =
        safeRequest { api.getItemDetails(id).item.toDomain() }

    override suspend fun getSimilarItems(id: Int): RequestResult<List<Item>> =
        safeRequest { api.getSimilarItems(id).items.map { it.toDomain() } }

    override suspend fun getGenres(): RequestResult<List<Genre>> =
        safeRequest { api.getGenres().items.map { it.toDomain() } }

    override suspend fun getCountries(): RequestResult<List<Country>> =
        safeRequest { api.getCountries().items.map { it.toDomain() } }

    override suspend fun getCollections(page: Int): RequestResult<List<Collection>> =
        safeRequest { api.getCollections(page = page).items.map { it.toDomain() } }

    override suspend fun getCollectionItems(collectionId: Int, page: Int): RequestResult<CollectionPage> =
        safeRequest { api.getCollectionItems(collectionId, page).toDomain() }
}

/**
 * Короткие перегрузки без [SortOption] всегда сортируют по убыванию: «популярное», «лучшее»
 * и «свежее» читаются сверху вниз. kino.pub: минус-префикс = DESC (см. [SortOption.apiValue]).
 */
private val CatalogSort.descending: String get() = "-$apiValue"

/**
 * Разворачивает доменные [CatalogFilters] в параметры `api/v1/items`. Диапазоны года и пороги
 * рейтингов уходят повторяемыми `conditions[]`, страна/качество/завершённость — отдельными
 * параметрами (так их принимает kino.pub).
 */
private fun CatalogFilters.toQuery(
    type: ItemType,
    genreId: Int?,
    sort: SortOption,
    page: Int,
): ItemsQuery = ItemsQuery(
    type = type.apiValue,
    sort = sort.apiValue,
    page = page,
    genreId = genreId,
    countryId = countryId,
    quality = if (only4k) QUALITY_4K else null,
    // finished=1 — только завершённые, finished=0 — только продолжающиеся, отсутствие — любые.
    finished = onlyFinished?.let { if (it) 1 else 0 },
    conditions = buildConditions(),
)

private fun CatalogFilters.buildConditions(): List<String> = buildList {
    yearFrom?.let { add("year>=$it") }
    yearTo?.let { add("year<=$it") }
    kpRatingFrom?.let { add("kinopoisk_rating>=$it") }
    imdbRatingFrom?.let { add("imdb_rating>=$it") }
}

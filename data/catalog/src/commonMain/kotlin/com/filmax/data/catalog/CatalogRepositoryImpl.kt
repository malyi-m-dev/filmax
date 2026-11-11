package com.filmax.data.catalog

import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.CatalogShortcut
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.GenreFilter
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.CollectionPage
import com.filmax.core.domain.catalog.model.Country
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemPage
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.RatingSource
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
        safeRequest { api.getItems(type.kinopubValue, SortOption(sort).kinopubSort, page).toDomain() }

    override suspend fun getItems(
        type: ItemType,
        genre: GenreFilter?,
        filters: CatalogFilters,
        sort: SortOption,
        page: Int,
    ): RequestResult<ItemPage> =
        safeRequest { api.getFilteredItems(filters.toQuery(type, genre, sort, page)).toDomain() }

    override suspend fun getShortcutItems(
        shortcut: CatalogShortcut,
        type: ItemType,
        page: Int,
    ): RequestResult<ItemPage> =
        safeRequest { api.getItemsByShortcut(shortcut.kinopubPath, type.kinopubValue, page).toDomain() }

    override suspend fun getItemDetails(id: Int): RequestResult<Item> =
        safeRequest { api.getItemDetails(id).item.toDomain() }

    // distinctBy(id) здесь и в подборках: сервер может отдать тайтл дважды, а списки уходят
    // в Lazy-контейнеры с key = id — дубликат ключа роняет Compose («Key … was already used»).
    override suspend fun getSimilarItems(id: Int): RequestResult<List<Item>> =
        safeRequest { api.getSimilarItems(id).items.map { it.toDomain() }.distinctBy { it.id } }

    override suspend fun getGenres(): RequestResult<List<Genre>> =
        safeRequest { api.getGenres().items.map { it.toDomain() } }

    override suspend fun getCountries(): RequestResult<List<Country>> =
        safeRequest { api.getCountries().items.map { it.toDomain() } }

    override suspend fun getCollections(page: Int): RequestResult<List<Collection>> =
        safeRequest { api.getCollections(page = page).items.map { it.toDomain() }.distinctBy { it.id } }

    override suspend fun getCollectionItems(collectionId: Int, page: Int): RequestResult<CollectionPage> =
        safeRequest {
            val collectionPage = api.getCollectionItems(collectionId, page).toDomain()
            collectionPage.copy(items = collectionPage.items.distinctBy { it.id })
        }
}

/**
 * Разворачивает доменные [CatalogFilters] в параметры `api/v1/items`. Диапазоны года и пороги
 * рейтингов уходят повторяемыми `conditions[]`, страна/качество/завершённость — отдельными
 * параметрами (так их принимает kino.pub).
 */
private fun CatalogFilters.toQuery(
    type: ItemType,
    genre: GenreFilter?,
    sort: SortOption,
    page: Int,
): ItemsQuery = ItemsQuery(
    type = type.kinopubValue,
    sort = sort.kinopubSort,
    page = page,
    genreId = when (genre) {
        null -> null
        is GenreFilter.Listed -> genre.id
        is GenreFilter.Reference -> genre.genre.kinopubId
    },
    countryId = countryId,
    quality = if (only4k) QUALITY_4K else null,
    // finished=1 — только завершённые, finished=0 — только продолжающиеся, отсутствие — любые.
    finished = onlyFinished?.let { if (it) 1 else 0 },
    conditions = buildConditions(),
)

private fun CatalogFilters.buildConditions(): List<String> = buildList {
    yearFrom?.let { add("year>=$it") }
    yearTo?.let { add("year<=$it") }
    minRating(RatingSource.KINOPOISK)?.let { add("kinopoisk_rating>=$it") }
    minRating(RatingSource.IMDB)?.let { add("imdb_rating>=$it") }
}

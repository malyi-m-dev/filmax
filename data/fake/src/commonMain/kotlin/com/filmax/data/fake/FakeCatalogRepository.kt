package com.filmax.data.fake

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
import com.filmax.data.fake.store.CatalogQuery
import com.filmax.data.fake.store.pageOf

/**
 * Каталог на фикстурах: фильтры, сортировка с направлением и честная пагинация по 20 — как у
 * `api/v1/items`. «Горячее» — по просмотрам, «новое» — по дате добавления.
 */
// Тот же связный контракт, что у CatalogRepositoryImpl: столько же методов, дробить его незачем.
@Suppress("TooManyFunctions")
class FakeCatalogRepository(private val backend: FakeBackend) : CatalogRepository {

    override suspend fun getItems(type: ItemType, sort: CatalogSort, page: Int): RequestResult<ItemPage> =
        browse(CatalogQuery(type = type, sort = SortOption(sort)), page)

    override suspend fun getItemsByGenre(
        type: ItemType,
        genreId: Int,
        sort: CatalogSort,
        page: Int,
    ): RequestResult<ItemPage> =
        browse(CatalogQuery(type = type, genreId = genreId, sort = SortOption(sort)), page)

    override suspend fun getItems(
        type: ItemType,
        genreId: Int?,
        filters: CatalogFilters,
        sort: SortOption,
        page: Int,
    ): RequestResult<ItemPage> =
        browse(CatalogQuery(type = type, genreId = genreId, filters = filters, sort = sort), page)

    override suspend fun getHotItems(type: ItemType, page: Int): RequestResult<ItemPage> =
        browse(CatalogQuery(type = type, sort = SortOption(CatalogSort.VIEWS)), page)

    override suspend fun getNewItems(type: ItemType, page: Int): RequestResult<ItemPage> =
        browse(CatalogQuery(type = type, sort = SortOption(CatalogSort.CREATED)), page)

    override suspend fun getItemDetails(id: Int): RequestResult<Item> = backend.respond {
        catalog.entry(id)?.let { entry -> success(detailedItem(entry)) } ?: notFound("item $id")
    }

    override suspend fun getSimilarItems(id: Int): RequestResult<List<Item>> = backend.respond {
        catalog.entry(id)
            ?.let { entry -> success(catalog.similarTo(entry).map { listItem(it) }) }
            ?: notFound("item $id")
    }

    override suspend fun getGenres(): RequestResult<List<Genre>> = backend.respond { success(catalog.genres) }

    override suspend fun getCountries(): RequestResult<List<Country>> = backend.respond { success(catalog.countries) }

    // Подборок меньше страницы: вторая страница пустая, и ряд на главной на ней останавливается.
    override suspend fun getCollections(page: Int): RequestResult<List<Collection>> = backend.respond {
        success(catalog.collections.pageOf(page).map { it.collection })
    }

    override suspend fun getCollectionItems(collectionId: Int, page: Int): RequestResult<CollectionPage> =
        backend.respond {
            val found = catalog.collection(collectionId) ?: return@respond notFound("collection $collectionId")
            val itemPage = itemPage(found.itemIds.mapNotNull { catalog.entry(it) }, page)
            success(CollectionPage(found.collection, itemPage.items, itemPage.pagination))
        }

    private suspend fun browse(query: CatalogQuery, page: Int): RequestResult<ItemPage> =
        backend.respond { success(itemPage(catalog.browse(query), page)) }
}

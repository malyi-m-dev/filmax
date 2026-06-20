package com.filmax.data.catalog

import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.CollectionPage
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemPage
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.common.safeRequest
import com.filmax.data.catalog.mapper.toDomain
import com.filmax.data.catalog.remote.CatalogApi
import javax.inject.Inject

internal class CatalogRepositoryImpl @Inject constructor(
    private val api: CatalogApi,
) : CatalogRepository {

    override suspend fun getItems(type: ItemType, sort: CatalogSort, page: Int): RequestResult<ItemPage> =
        safeRequest { api.getItems(type.apiValue, sort.apiValue, page).toDomain() }

    override suspend fun getItemsByGenre(
        type: ItemType,
        genreId: Int,
        sort: CatalogSort,
        page: Int,
    ): RequestResult<ItemPage> =
        safeRequest { api.getItemsByGenre(type.apiValue, genreId, sort.apiValue, page).toDomain() }

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

    override suspend fun getCollections(page: Int): RequestResult<List<Collection>> =
        safeRequest { api.getCollections(page = page).items.map { it.toDomain() } }

    override suspend fun getCollectionItems(collectionId: Int, page: Int): RequestResult<CollectionPage> =
        safeRequest { api.getCollectionItems(collectionId, page).toDomain() }
}

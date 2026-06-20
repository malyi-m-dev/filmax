package com.filmax.data.catalog

import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.CollectionPage
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemPage
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.data.catalog.mapper.toDomain
import com.filmax.data.catalog.remote.CatalogApi
import javax.inject.Inject

internal class CatalogRepositoryImpl @Inject constructor(
    private val api: CatalogApi,
) : CatalogRepository {

    override suspend fun getItems(type: ItemType, sort: CatalogSort, page: Int): ItemPage =
        api.getItems(type.apiValue, sort.apiValue, page).toDomain()

    override suspend fun getItemsByGenre(
        type: ItemType,
        genreId: Int,
        sort: CatalogSort,
        page: Int,
    ): ItemPage = api.getItemsByGenre(type.apiValue, genreId, sort.apiValue, page).toDomain()

    override suspend fun getHotItems(type: ItemType, page: Int): ItemPage =
        api.getItemsByShortcut("hot", type.apiValue, page).toDomain()

    override suspend fun getNewItems(type: ItemType, page: Int): ItemPage =
        api.getItemsByShortcut("new", type.apiValue, page).toDomain()

    override suspend fun getItemDetails(id: Int): Item =
        api.getItemDetails(id).item.toDomain()

    override suspend fun getSimilarItems(id: Int): List<Item> =
        api.getSimilarItems(id).items.map { it.toDomain() }

    override suspend fun getGenres(): List<Genre> =
        api.getGenres().items.map { it.toDomain() }

    override suspend fun getCollections(page: Int): List<Collection> =
        api.getCollections(page = page).items.map { it.toDomain() }

    override suspend fun getCollectionItems(collectionId: Int, page: Int): CollectionPage =
        api.getCollectionItems(collectionId, page).toDomain()
}

package com.filmax.core.domain.catalog

import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.CollectionPage
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemPage
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.common.RequestResult

interface CatalogRepository {

    suspend fun getItems(
        type: ItemType,
        sort: CatalogSort = CatalogSort.UPDATED,
        page: Int = 1,
    ): RequestResult<ItemPage>

    suspend fun getItemsByGenre(
        type: ItemType,
        genreId: Int,
        sort: CatalogSort = CatalogSort.UPDATED,
        page: Int = 1,
    ): RequestResult<ItemPage>

    suspend fun getHotItems(type: ItemType, page: Int = 1): RequestResult<ItemPage>

    suspend fun getNewItems(type: ItemType, page: Int = 1): RequestResult<ItemPage>

    suspend fun getItemDetails(id: Int): RequestResult<Item>

    suspend fun getSimilarItems(id: Int): RequestResult<List<Item>>

    suspend fun getGenres(): RequestResult<List<Genre>>

    suspend fun getCollections(page: Int = 1): RequestResult<List<Collection>>

    suspend fun getCollectionItems(collectionId: Int, page: Int = 1): RequestResult<CollectionPage>
}

enum class CatalogSort(val apiValue: String) {
    UPDATED("updated"),
    CREATED("created"),
    RATING("rating"),
    VIEWS("views"),
    YEAR("year"),
}

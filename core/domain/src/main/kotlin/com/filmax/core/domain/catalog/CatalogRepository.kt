package com.filmax.core.domain.catalog

import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.CollectionPage
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemPage
import com.filmax.core.domain.catalog.model.ItemType

interface CatalogRepository {

    suspend fun getItems(
        type: ItemType,
        sort: CatalogSort = CatalogSort.UPDATED,
        page: Int = 1,
    ): ItemPage

    suspend fun getItemsByGenre(
        type: ItemType,
        genreId: Int,
        sort: CatalogSort = CatalogSort.UPDATED,
        page: Int = 1,
    ): ItemPage

    suspend fun getHotItems(type: ItemType, page: Int = 1): ItemPage

    suspend fun getNewItems(type: ItemType, page: Int = 1): ItemPage

    suspend fun getItemDetails(id: Int): Item

    suspend fun getSimilarItems(id: Int): List<Item>

    suspend fun getGenres(): List<Genre>

    suspend fun getCollections(page: Int = 1): List<Collection>

    suspend fun getCollectionItems(collectionId: Int, page: Int = 1): CollectionPage
}

enum class CatalogSort(val apiValue: String) {
    UPDATED("updated"),
    CREATED("created"),
    RATING("rating"),
    VIEWS("views"),
    YEAR("year"),
}

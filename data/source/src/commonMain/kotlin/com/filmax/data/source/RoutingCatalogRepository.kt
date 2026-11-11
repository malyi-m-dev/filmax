package com.filmax.data.source

import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.CatalogShortcut
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

// Переключатель повторяет контракт каталога метод в метод — столько же функций, сколько в нём.
@Suppress("TooManyFunctions")
internal class RoutingCatalogRepository(private val switch: SourceSwitch<CatalogRepository>) : CatalogRepository {

    override suspend fun getItems(type: ItemType, sort: CatalogSort, page: Int): RequestResult<ItemPage> =
        switch.current.getItems(type, sort, page)

    override suspend fun getItemsByGenre(
        type: ItemType,
        genreId: Int,
        sort: CatalogSort,
        page: Int,
    ): RequestResult<ItemPage> = switch.current.getItemsByGenre(type, genreId, sort, page)

    override suspend fun getItems(
        type: ItemType,
        genreId: Int?,
        filters: CatalogFilters,
        sort: SortOption,
        page: Int,
    ): RequestResult<ItemPage> = switch.current.getItems(type, genreId, filters, sort, page)

    override suspend fun getShortcutItems(
        shortcut: CatalogShortcut,
        type: ItemType,
        page: Int,
    ): RequestResult<ItemPage> = switch.current.getShortcutItems(shortcut, type, page)

    override suspend fun getItemDetails(id: Int): RequestResult<Item> = switch.current.getItemDetails(id)

    override suspend fun getSimilarItems(id: Int): RequestResult<List<Item>> = switch.current.getSimilarItems(id)

    override suspend fun getGenres(): RequestResult<List<Genre>> = switch.current.getGenres()

    override suspend fun getCountries(): RequestResult<List<Country>> = switch.current.getCountries()

    override suspend fun getCollections(page: Int): RequestResult<List<Collection>> =
        switch.current.getCollections(page)

    override suspend fun getCollectionItems(collectionId: Int, page: Int): RequestResult<CollectionPage> =
        switch.current.getCollectionItems(collectionId, page)
}

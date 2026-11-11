package com.filmax.data.jellyfin

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
import com.filmax.core.domain.catalog.model.Pagination
import com.filmax.core.domain.common.RequestResult
import com.filmax.data.jellyfin.mapper.JellyfinMapper
import com.filmax.data.jellyfin.remote.ItemsQuery
import com.filmax.data.jellyfin.remote.dto.ItemsResponseDto

/**
 * Каталог домашнего сервера. Подборки — коллекции сервера (BoxSet): их собирают в Jellyfin
 * вручную или плагином, по смыслу это то же, что подборки kino.pub.
 */
// Реализация всего контракта CatalogRepository — столько же методов, дробить незачем.
@Suppress("TooManyFunctions")
internal class JellyfinCatalogRepository(
    private val context: JellyfinContext,
    private val videos: JellyfinVideos,
) : CatalogRepository {

    private val api = context.api
    private val ids = context.ids

    override suspend fun getItems(type: ItemType, sort: CatalogSort, page: Int): RequestResult<ItemPage> =
        page(type.toQuery().copy(sortBy = sort.sortBy(type)), page)

    override suspend fun getItemsByGenre(
        type: ItemType,
        genreId: Int,
        sort: CatalogSort,
        page: Int,
    ): RequestResult<ItemPage> = context.request { mapper ->
        val query = type.toQuery().copy(sortBy = sort.sortBy(type)).withGenre(genreId, ids)
        itemPage(api.items(query.paged(page)), page, mapper)
    }

    override suspend fun getItems(
        type: ItemType,
        genreId: Int?,
        filters: CatalogFilters,
        sort: SortOption,
        page: Int,
    ): RequestResult<ItemPage> = context.request { mapper ->
        val query = type.toQuery().filtered(filters)
            .copy(sortBy = sort.field.sortBy(type), ascending = sort.ascending)
            .withGenre(genreId, ids)
        itemPage(api.items(query.paged(page)), page, mapper)
    }

    override suspend fun getShortcutItems(
        shortcut: CatalogShortcut,
        type: ItemType,
        page: Int,
    ): RequestResult<ItemPage> = page(shortcut.toQuery(type), page)

    /** Тайтл с сериями: у сериала — все серии всех сезонов, кроме спецвыпусков. */
    override suspend fun getItemDetails(id: Int): RequestResult<Item> = context.request { mapper ->
        val title = videos.title(ids.requireGuid(id))
        val tracklist = if (title.episodes.isEmpty()) {
            listOf(mapper.movieTrack(title.item))
        } else {
            title.episodes.mapNotNull(mapper::episode)
        }
        mapper.item(title.item, tracklist)
    }

    override suspend fun getSimilarItems(id: Int): RequestResult<List<Item>> = context.request { mapper ->
        api.similar(ids.requireGuid(id), SIMILAR_LIMIT).items.orEmpty().map(mapper::item).distinctBy { it.id }
    }

    override suspend fun getGenres(): RequestResult<List<Genre>> = context.request { mapper ->
        api.genres(MOVIES_AND_SERIES).items.orEmpty().mapNotNull { mapper.genre(it.id, it.name) }.distinctBy { it.id }
    }

    override suspend fun getCountries(): RequestResult<List<Country>> = RequestResult.Success(emptyList())

    override suspend fun getCollections(page: Int): RequestResult<List<Collection>> = context.request { mapper ->
        val query = ItemsQuery(includeItemTypes = listOf(TYPE_BOX_SET), sortBy = listOf(SORT_NAME), ascending = true)
        api.items(query.paged(page)).items.orEmpty().map(mapper::collection).distinctBy { it.id }
    }

    override suspend fun getCollectionItems(collectionId: Int, page: Int): RequestResult<CollectionPage> =
        context.request { mapper ->
            val guid = ids.requireGuid(collectionId)
            val query = ItemsQuery(parentId = guid, sortBy = listOf(PRODUCTION_YEAR, SORT_NAME), ascending = true)
            val itemPage = itemPage(api.items(query.paged(page)), page, mapper)
            CollectionPage(
                collection = mapper.collection(api.item(guid)),
                items = itemPage.items,
                pagination = itemPage.pagination,
            )
        }

    private suspend fun page(query: ItemsQuery, page: Int): RequestResult<ItemPage> = context.request { mapper ->
        itemPage(api.items(query.paged(page)), page, mapper)
    }

    private fun itemPage(response: ItemsResponseDto, page: Int, mapper: JellyfinMapper): ItemPage {
        val total = response.totalRecordCount ?: 0
        return ItemPage(
            items = response.items.orEmpty().map(mapper::item).distinctBy { it.id },
            pagination = Pagination(
                total = maxOf(1, (total + PAGE_SIZE - 1) / PAGE_SIZE),
                current = page,
                perPage = PAGE_SIZE,
            ),
        )
    }

    private companion object {
        /** Тридцать — кратно и пяти, и шести колонкам сеток: последний ряд страницы полный. */
        const val PAGE_SIZE = 30
        const val SIMILAR_LIMIT = 12
        const val SORT_NAME = "SortName"
        const val PRODUCTION_YEAR = "ProductionYear"

        fun ItemsQuery.paged(page: Int) = copy(startIndex = (page.coerceAtLeast(1) - 1) * PAGE_SIZE, limit = PAGE_SIZE)
    }
}

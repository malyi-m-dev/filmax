package com.filmax.data.catalog.remote

import com.filmax.data.catalog.remote.dto.CollectionItemsDto
import com.filmax.data.catalog.remote.dto.CollectionsResponseDto
import com.filmax.data.catalog.remote.dto.GenresResponseDto
import com.filmax.data.catalog.remote.dto.ItemsResponseDto
import com.filmax.data.catalog.remote.dto.MovieInfoDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

internal class CatalogApi(private val client: HttpClient) {

    suspend fun getItemDetails(id: Int): MovieInfoDto =
        client.get("api/v1/items/$id").body()

    suspend fun getItems(type: String, sort: String, page: Int): ItemsResponseDto =
        client.get("api/v1/items") {
            parameter("type", type)
            parameter("sort", sort)
            parameter("page", page)
        }.body()

    suspend fun getItemsByGenre(type: String, genreId: Int, sort: String, page: Int): ItemsResponseDto =
        client.get("api/v1/items") {
            parameter("type", type)
            parameter("genre", genreId)
            parameter("sort", sort)
            parameter("page", page)
        }.body()

    suspend fun getItemsByShortcut(shortcut: String, type: String, page: Int): ItemsResponseDto =
        client.get("api/v1/items/$shortcut") {
            parameter("type", type)
            parameter("page", page)
        }.body()

    suspend fun getSimilarItems(id: Int): ItemsResponseDto =
        client.get("api/v1/items/similar") {
            parameter("id", id)
        }.body()

    suspend fun getGenres(): GenresResponseDto =
        client.get("api/v1/genres").body()

    suspend fun getCollections(sort: String? = null, page: Int): CollectionsResponseDto =
        client.get("api/v1/collections") {
            sort?.let { parameter("sort", it) }
            parameter("page", page)
        }.body()

    suspend fun getCollectionItems(id: Int, page: Int): CollectionItemsDto =
        client.get("api/v1/collections/view") {
            parameter("id", id)
            parameter("page", page)
        }.body()
}

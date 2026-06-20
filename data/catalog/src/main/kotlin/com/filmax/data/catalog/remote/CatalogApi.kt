package com.filmax.data.catalog.remote

import com.filmax.data.catalog.remote.dto.CollectionItemsDto
import com.filmax.data.catalog.remote.dto.CollectionsResponseDto
import com.filmax.data.catalog.remote.dto.GenresResponseDto
import com.filmax.data.catalog.remote.dto.ItemsResponseDto
import com.filmax.data.catalog.remote.dto.MovieInfoDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CatalogApi {

    @GET("api/v1/items/{id}")
    suspend fun getItemDetails(@Path("id") id: Int): MovieInfoDto

    @GET("api/v1/items")
    suspend fun getItems(
        @Query("type") type: String,
        @Query("sort") sort: String,
        @Query("page") page: Int,
    ): ItemsResponseDto

    @GET("api/v1/items")
    suspend fun getItemsByGenre(
        @Query("type") type: String,
        @Query("genre") genreId: Int,
        @Query("sort") sort: String,
        @Query("page") page: Int,
    ): ItemsResponseDto

    @GET("api/v1/items/{shortcut}")
    suspend fun getItemsByShortcut(
        @Path("shortcut") shortcut: String,
        @Query("type") type: String,
        @Query("page") page: Int,
    ): ItemsResponseDto

    @GET("api/v1/items/similar")
    suspend fun getSimilarItems(@Query("id") id: Int): ItemsResponseDto

    @GET("api/v1/genres")
    suspend fun getGenres(): GenresResponseDto

    @GET("api/v1/collections")
    suspend fun getCollections(
        @Query("sort") sort: String? = null,
        @Query("page") page: Int,
    ): CollectionsResponseDto

    @GET("api/v1/collections/view")
    suspend fun getCollectionItems(
        @Query("id") id: Int,
        @Query("page") page: Int,
    ): CollectionItemsDto
}

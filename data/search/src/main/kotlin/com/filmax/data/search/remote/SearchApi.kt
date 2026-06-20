package com.filmax.data.search.remote

import com.filmax.data.catalog.remote.dto.ItemsResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface SearchApi {

    @GET("api/v1/items/search")
    suspend fun search(
        @Query("q")       query: String,
        @Query("type")    type: String? = null,
        @Query("perpage") perPage: Int = 20,
    ): ItemsResponseDto

    @GET("api/v1/items")
    suspend fun searchByActor(
        @Query("actor")   actor: String,
        @Query("perpage") perPage: Int = 20,
    ): ItemsResponseDto

    @GET("api/v1/items")
    suspend fun searchByDirector(
        @Query("director") director: String,
        @Query("perpage")  perPage: Int = 20,
    ): ItemsResponseDto
}

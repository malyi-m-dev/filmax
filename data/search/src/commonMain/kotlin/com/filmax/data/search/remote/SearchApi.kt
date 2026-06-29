package com.filmax.data.search.remote

import com.filmax.data.catalog.remote.dto.ItemsResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

internal class SearchApi(private val client: HttpClient) {

    suspend fun search(query: String, type: String? = null, perPage: Int = 20): ItemsResponseDto =
        client.get("api/v1/items/search") {
            parameter("q", query)
            type?.let { parameter("type", it) }
            parameter("perpage", perPage)
        }.body()

    suspend fun searchByActor(actor: String, perPage: Int = 20): ItemsResponseDto =
        client.get("api/v1/items") {
            parameter("actor", actor)
            parameter("perpage", perPage)
        }.body()

    suspend fun searchByDirector(director: String, perPage: Int = 20): ItemsResponseDto =
        client.get("api/v1/items") {
            parameter("director", director)
            parameter("perpage", perPage)
        }.body()
}

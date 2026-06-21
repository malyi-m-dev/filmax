package com.filmax.data.search

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.common.safeRequest
import com.filmax.core.domain.search.SearchRepository
import com.filmax.data.catalog.mapper.toDomain
import com.filmax.data.search.remote.SearchApi

internal class SearchRepositoryImpl(
    private val api: SearchApi,
) : SearchRepository {

    override suspend fun search(query: String, type: ItemType?, perPage: Int): RequestResult<List<Item>> =
        safeRequest { api.search(query, type?.apiValue, perPage).toDomain().items }

    override suspend fun searchByActor(actor: String, perPage: Int): RequestResult<List<Item>> =
        safeRequest { api.searchByActor(actor, perPage).toDomain().items }

    override suspend fun searchByDirector(director: String, perPage: Int): RequestResult<List<Item>> =
        safeRequest { api.searchByDirector(director, perPage).toDomain().items }
}

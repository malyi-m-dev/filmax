package com.filmax.data.search

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.search.SearchRepository
import com.filmax.data.catalog.mapper.toDomain
import com.filmax.data.search.remote.SearchApi
import javax.inject.Inject

internal class SearchRepositoryImpl @Inject constructor(
    private val api: SearchApi,
) : SearchRepository {

    override suspend fun search(query: String, type: ItemType?, perPage: Int): List<Item> =
        api.search(query, type?.apiValue, perPage).toDomain().items

    override suspend fun searchByActor(actor: String, perPage: Int): List<Item> =
        api.searchByActor(actor, perPage).toDomain().items

    override suspend fun searchByDirector(director: String, perPage: Int): List<Item> =
        api.searchByDirector(director, perPage).toDomain().items
}

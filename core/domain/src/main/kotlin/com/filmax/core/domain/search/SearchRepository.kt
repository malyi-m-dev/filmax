package com.filmax.core.domain.search

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.common.RequestResult

interface SearchRepository {

    suspend fun search(
        query: String,
        type: ItemType? = null,
        perPage: Int = 20,
    ): RequestResult<List<Item>>

    suspend fun searchByActor(actor: String, perPage: Int = 20): RequestResult<List<Item>>

    suspend fun searchByDirector(director: String, perPage: Int = 20): RequestResult<List<Item>>
}

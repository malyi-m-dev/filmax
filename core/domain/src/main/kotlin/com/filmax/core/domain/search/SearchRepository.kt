package com.filmax.core.domain.search

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType

interface SearchRepository {

    suspend fun search(
        query: String,
        type: ItemType? = null,
        perPage: Int = 20,
    ): List<Item>

    suspend fun searchByActor(actor: String, perPage: Int = 20): List<Item>

    suspend fun searchByDirector(director: String, perPage: Int = 20): List<Item>
}

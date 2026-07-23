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

    // distinctBy(id) во всех трёх: kino.pub может отдать тайтл дважды (актёр в нескольких ролях
    // одного тайтла, совпадение по двум названиям), а выдача идёт в Lazy-списки с key = id —
    // дубликат ключа роняет Compose («Key … was already used»).
    override suspend fun search(query: String, type: ItemType?, perPage: Int): RequestResult<List<Item>> =
        safeRequest { api.search(query, type?.apiValue, perPage).toDomain().items.distinctBy { it.id } }

    override suspend fun searchByActor(actor: String, perPage: Int): RequestResult<List<Item>> =
        safeRequest { api.searchByActor(actor, perPage).toDomain().items.distinctBy { it.id } }

    override suspend fun searchByDirector(director: String, perPage: Int): RequestResult<List<Item>> =
        safeRequest { api.searchByDirector(director, perPage).toDomain().items.distinctBy { it.id } }
}

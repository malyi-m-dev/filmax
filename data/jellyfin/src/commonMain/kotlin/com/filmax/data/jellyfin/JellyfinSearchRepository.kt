package com.filmax.data.jellyfin

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.search.SearchRepository
import com.filmax.data.jellyfin.remote.ItemsQuery

/** Поиск по медиатеке сервера: по названию и по людям из метаданных (актёры, режиссёры). */
internal class JellyfinSearchRepository(private val context: JellyfinContext) : SearchRepository {

    override suspend fun search(query: String, type: ItemType?, perPage: Int): RequestResult<List<Item>> {
        val base = type?.toQuery() ?: ItemsQuery(includeItemTypes = MOVIES_AND_SERIES)
        return find(base.copy(searchTerm = query, limit = perPage))
    }

    override suspend fun searchByActor(actor: String, perPage: Int): RequestResult<List<Item>> =
        find(byPerson(actor, ACTOR, perPage))

    override suspend fun searchByDirector(director: String, perPage: Int): RequestResult<List<Item>> =
        find(byPerson(director, DIRECTOR, perPage))

    private suspend fun find(query: ItemsQuery): RequestResult<List<Item>> = context.request { mapper ->
        context.api.items(query).items.orEmpty().map(mapper::item).distinctBy { it.id }
    }

    private fun byPerson(name: String, role: String, limit: Int) = ItemsQuery(
        includeItemTypes = MOVIES_AND_SERIES,
        person = name,
        personTypes = listOf(role),
        sortBy = listOf(PRODUCTION_YEAR),
        limit = limit,
    )

    private companion object {
        const val ACTOR = "Actor"
        const val DIRECTOR = "Director"
        const val PRODUCTION_YEAR = "ProductionYear"
    }
}

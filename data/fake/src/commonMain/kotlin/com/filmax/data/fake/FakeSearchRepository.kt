package com.filmax.data.fake

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.search.SearchRepository
import com.filmax.data.fake.store.CatalogEntry

/**
 * Поиск по фикстурам: по подстроке названия без учёта регистра, фильмография — по вхождению имени
 * в `cast`/`director`. Тайтлы выдуманы, поэтому «тренды» экрана поиска («Дюна», «Офис») здесь
 * честно ничего не находят — заодно видно пустое состояние.
 */
class FakeSearchRepository(private val backend: FakeBackend) : SearchRepository {

    override suspend fun search(query: String, type: ItemType?, perPage: Int): RequestResult<List<Item>> =
        searchResult(perPage) { catalog.searchByTitle(query, type) }

    override suspend fun searchByActor(actor: String, perPage: Int): RequestResult<List<Item>> =
        searchResult(perPage) { catalog.withActor(actor) }

    override suspend fun searchByDirector(director: String, perPage: Int): RequestResult<List<Item>> =
        searchResult(perPage) { catalog.withDirector(director) }

    private suspend fun searchResult(
        perPage: Int,
        query: FakeBackend.() -> List<CatalogEntry>,
    ): RequestResult<List<Item>> =
        backend.respond { success(query().take(perPage).map { listItem(it) }) }
}

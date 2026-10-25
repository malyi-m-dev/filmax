package com.filmax.feature.home.common

import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.CatalogShortcut
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.ItemPage
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.common.RequestResult
import kotlinx.coroutines.CompletableDeferred

/** Запрос выдачи тайтлов — в терминах источника ряда, чтобы тест сравнивал его с [TITLE_SOURCES]. */
internal data class TitlesCall(val source: TitleSource, val page: Int)

/**
 * Каталог-фейк Главной с записью запросов выдачи. [held] — источники, чьи ответы ждут [release]:
 * так видно, что лента уже на экране, а тематические ряды ещё в пути. [failing] — источники, которые
 * отвечают ошибкой. [endless] — у любой выдачи есть следующая страница: у фикстур в ряду меньше
 * страницы, и без этого догрузку ряда не проверить.
 */
internal class RecordingHomeCatalog(
    private val delegate: CatalogRepository,
    private val held: Set<TitleSource> = emptySet(),
    var failing: Set<TitleSource> = emptySet(),
    private val endless: Boolean = false,
) : CatalogRepository by delegate {

    val calls = mutableListOf<TitlesCall>()

    private val gate = CompletableDeferred<Unit>()

    /** Отпустить придержанные ответы. */
    fun release() {
        gate.complete(Unit)
    }

    override suspend fun getItems(type: ItemType, sort: CatalogSort, page: Int): RequestResult<ItemPage> =
        answer(TitleSource.Catalog(type, sort), page) { delegate.getItems(type, sort, page) }

    override suspend fun getItems(
        type: ItemType,
        genreId: Int?,
        filters: CatalogFilters,
        sort: SortOption,
        page: Int,
    ): RequestResult<ItemPage> =
        answer(TitleSource.Catalog(type, sort.field, genreId), page) {
            delegate.getItems(type, genreId, filters, sort, page)
        }

    override suspend fun getShortcutItems(
        shortcut: CatalogShortcut,
        type: ItemType,
        page: Int,
    ): RequestResult<ItemPage> =
        answer(TitleSource.Shortcut(type, shortcut), page) { delegate.getShortcutItems(shortcut, type, page) }

    private suspend fun answer(
        source: TitleSource,
        page: Int,
        request: suspend () -> RequestResult<ItemPage>,
    ): RequestResult<ItemPage> {
        calls += TitlesCall(source, page)
        if (source in held) gate.await()
        val result: RequestResult<ItemPage> = if (source in failing) RequestResult.Error(OFFLINE_MESSAGE) else request()
        return if (endless && result is RequestResult.Success) result.withNextPage() else result
    }

    /** Та же страница, но сервер обещает за ней следующую. */
    private fun RequestResult.Success<ItemPage>.withNextPage(): RequestResult<ItemPage> {
        val pagination = data.pagination
        return RequestResult.Success(data.copy(pagination = pagination.copy(total = pagination.current + 1)))
    }

    private companion object {
        const val OFFLINE_MESSAGE = "нет сети"
    }
}

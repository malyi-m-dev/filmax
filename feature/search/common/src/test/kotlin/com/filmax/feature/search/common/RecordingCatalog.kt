package com.filmax.feature.search.common

import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.ItemPage
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.common.RequestResult
import kotlinx.coroutines.delay

/**
 * Каталог-фейк с записью запросов выдачи: тесты проверяют, что витрина НЕ перезапрашивается, где
 * не должна. [latencyOf] — задержка ответа по типу: так быстрые переключения чипов приходят не
 * в том порядке, в каком их запросили.
 */
internal class RecordingCatalog(
    private val delegate: CatalogRepository,
    private val latencyOf: (ItemType) -> Long = { 0L },
) : CatalogRepository by delegate {

    /** Запросы выдачи: тип и номер страницы. */
    val requests = mutableListOf<Pair<ItemType, Int>>()

    /** Сеть пропала: выдача отвечает ошибкой. */
    var offline = false

    override suspend fun getItems(
        type: ItemType,
        genreId: Int?,
        filters: CatalogFilters,
        sort: SortOption,
        page: Int,
    ): RequestResult<ItemPage> {
        requests += type to page
        delay(latencyOf(type))
        if (offline) return RequestResult.Error(OFFLINE_MESSAGE)
        return delegate.getItems(type, genreId, filters, sort, page)
    }

    companion object {
        const val OFFLINE_MESSAGE = "нет сети"
    }
}

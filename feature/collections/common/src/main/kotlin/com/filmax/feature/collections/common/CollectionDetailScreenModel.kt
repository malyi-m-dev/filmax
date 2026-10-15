package com.filmax.feature.collections.common

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.presentation.BaseScreenModel
import com.filmax.feature.collections.common.navigation.CollectionDetailRoute

class CollectionDetailScreenModel(
    savedStateHandle: SavedStateHandle,
    private val catalog: CatalogRepository,
) : BaseScreenModel<CollectionDetailState, CollectionDetailSideEffect, CollectionDetailEvent>(
    CollectionDetailState(),
) {

    private val route = savedStateHandle.toRoute<CollectionDetailRoute>()

    init {
        onFetchData()
    }

    override fun onEvent(event: CollectionDetailEvent) {
        when (event) {
            CollectionDetailEvent.LoadMore -> loadMore()
            is CollectionDetailEvent.FocusRow -> focusRow(event.row)
        }
    }

    /** Повтор той же строки — не событие: фокус ходит по ней влево-вправо, шапка от этого не меняется. */
    private fun focusRow(row: Int?) {
        if (row == state.focusedRow) return
        screenModelScope { _ -> updateState { it.copy(focusedRow = row) } }
    }

    override fun onFetchData() {
        screenModelScope { _ ->
            updateState { it.copy(loading = true, error = null) }
            when (val result = catalog.getCollectionItems(route.collectionId, page = 1)) {
                is RequestResult.Success ->
                    updateState {
                        it.copy(
                            loading = false,
                            items = result.data.items.distinctBy(Item::id),
                            page = 1,
                            hasNextPage = result.data.pagination.hasNextPage,
                            loadingMore = false,
                            cover = result.data.collection?.coverUrl(),
                        )
                    }

                is RequestResult.Error -> {
                    updateState { it.copy(loading = false, error = result.message) }
                    showError(result)
                }
            }
        }
    }

    /**
     * Следующая страница подборки. Идемпотентна: UI дёргает её на каждом подходе к хвосту сетки,
     * а пока страница в пути или она последняя — повторный вызов ничего не делает.
     */
    private fun loadMore() {
        val current = state
        if (current.loading || current.loadingMore || !current.hasNextPage) return
        val nextPage = current.page + 1
        screenModelScope { _ ->
            updateState { it.copy(loadingMore = true) }
            when (val result = catalog.getCollectionItems(route.collectionId, page = nextPage)) {
                is RequestResult.Success -> updateState { loaded ->
                    loaded.copy(
                        loadingMore = false,
                        // Дубли гасим по id: LazyGrid падает на неуникальном ключе, а границы
                        // страниц у kino.pub плывут, если подборку правят во время просмотра.
                        items = (loaded.items + result.data.items).distinctBy(Item::id),
                        page = nextPage,
                        hasNextPage = result.data.pagination.hasNextPage,
                    )
                }

                // Тихий фейл: показанное не рушим, следующий подход к хвосту повторит.
                is RequestResult.Error -> updateState { it.copy(loadingMore = false) }
            }
        }
    }
}

/**
 * Кадр для шапки подборки: широкий, если kino.pub его прислал, иначе большой постер. Пустая строка —
 * не картинка: kino.pub отдаёт поле, даже когда файла нет.
 */
private fun Collection.coverUrl(): String? = posters?.let { posters ->
    posters.wide?.takeIf { it.isNotBlank() } ?: posters.big.ifBlank { posters.medium }
}?.takeIf { it.isNotBlank() }

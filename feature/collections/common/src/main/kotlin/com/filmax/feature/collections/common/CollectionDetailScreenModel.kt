package com.filmax.feature.collections.common

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.filmax.core.domain.catalog.CatalogRepository
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

    override fun dispatch(event: CollectionDetailEvent) {
        when (event) {
            CollectionDetailEvent.LoadMore -> loadMore()
        }
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
        screenModelScope { _ ->
            updateState { it.copy(loadingMore = true) }
            when (val result = catalog.getCollectionItems(route.collectionId, page = current.page + 1)) {
                is RequestResult.Success -> updateState { s ->
                    // Дубли гасим по id: LazyGrid падает на неуникальном ключе, а границы
                    // страниц у kino.pub плывут, если подборку правят во время просмотра.
                    val seen = s.items.mapTo(HashSet(), Item::id)
                    s.copy(
                        loadingMore = false,
                        items = s.items + result.data.items.filter { it.id !in seen }.distinctBy(Item::id),
                        page = current.page + 1,
                        hasNextPage = result.data.pagination.hasNextPage,
                    )
                }

                // Тихий фейл: показанное не рушим, следующий подход к хвосту повторит.
                is RequestResult.Error -> updateState { it.copy(loadingMore = false) }
            }
        }
    }
}

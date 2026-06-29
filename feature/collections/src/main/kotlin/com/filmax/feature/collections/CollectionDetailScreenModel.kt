package com.filmax.feature.collections

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.presentation.BaseScreenModel
import com.filmax.feature.collections.navigation.CollectionDetailRoute

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

    override fun dispatch(event: CollectionDetailEvent) = Unit

    override fun onFetchData() {
        screenModelScope {
            updateState { it.copy(loading = true, error = null) }
            when (val result = catalog.getCollectionItems(route.collectionId, page = 1)) {
                is RequestResult.Success ->
                    updateState { it.copy(loading = false, items = result.data.items) }

                is RequestResult.Error -> {
                    updateState { it.copy(loading = false, error = result.message) }
                    showError(result)
                }
            }
        }
    }
}

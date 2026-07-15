package com.filmax.feature.collections.common

import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.presentation.BaseScreenModel

class CollectionsScreenModel(
    private val catalog: CatalogRepository,
) : BaseScreenModel<CollectionsState, CollectionsSideEffect, CollectionsEvent>(CollectionsState()) {

    init {
        onFetchData()
    }

    override fun dispatch(event: CollectionsEvent) {
        when (event) {
            is CollectionsEvent.QueryChange -> screenModelScope { _ ->
                updateState { it.copy(query = event.query) }
            }
        }
    }

    override fun onFetchData() {
        screenModelScope { _ ->
            when (val result = catalog.getCollections()) {
                is RequestResult.Success ->
                    updateState { it.copy(loading = false, collections = result.data) }

                is RequestResult.Error ->
                    updateState { it.copy(loading = false, error = result.message) }
            }
        }
    }
}

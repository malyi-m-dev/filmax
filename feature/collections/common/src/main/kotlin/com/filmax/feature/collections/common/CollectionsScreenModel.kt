package com.filmax.feature.collections.common

import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.model.Collection
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
            is CollectionsEvent.CollectionClick -> onCollectionClick(event.collection)
            CollectionsEvent.DismissSheet -> screenModelScope { _ ->
                updateState { it.copy(selectedCollection = null, collectionItems = emptyList()) }
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

    private fun onCollectionClick(collection: Collection) {
        screenModelScope { _ ->
            updateState {
                it.copy(
                    selectedCollection = collection,
                    collectionItems = emptyList(),
                    loadingItems = true,
                )
            }
            when (val result = catalog.getCollectionItems(collection.id, page = 1)) {
                is RequestResult.Success ->
                    updateState { it.copy(loadingItems = false, collectionItems = result.data.items) }

                is RequestResult.Error ->
                    updateState { it.copy(loadingItems = false, error = result.message) }
            }
        }
    }
}

package com.filmax.feature.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.common.RequestResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CollectionsViewModel(
    private val catalog: CatalogRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CollectionsUiState())
    val state = _state.asStateFlow()

    init {
        loadCollections()
    }

    private fun loadCollections() {
        viewModelScope.launch {
            when (val result = catalog.getCollections()) {
                is RequestResult.Success ->
                    _state.update { it.copy(loading = false, collections = result.data) }

                is RequestResult.Error ->
                    _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun onCollectionClick(collection: Collection) {
        _state.update {
            it.copy(selectedCollection = collection, collectionItems = emptyList(), loadingItems = true)
        }
        viewModelScope.launch {
            when (val result = catalog.getCollectionItems(collection.id, page = 1)) {
                is RequestResult.Success ->
                    _state.update { it.copy(loadingItems = false, collectionItems = result.data.items) }

                is RequestResult.Error ->
                    _state.update { it.copy(loadingItems = false, error = result.message) }
            }
        }
    }

    fun onDismissSheet() {
        _state.update { it.copy(selectedCollection = null, collectionItems = emptyList()) }
    }
}

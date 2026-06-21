package com.filmax.feature.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.common.onSuccess
import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.feature.details.navigation.DetailsRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailsUiState(
    val loading: Boolean = true,
    val item: Item? = null,
    val similar: List<Item> = emptyList(),
    val isFav: Boolean = false,
    val error: String? = null,
)

class DetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val catalog: CatalogRepository,
    private val watching: WatchingRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<DetailsRoute>()
    private val _state = MutableStateFlow(DetailsUiState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val itemResult = catalog.getItemDetails(route.itemId)
            val similar = catalog.getSimilarItems(route.itemId).getOrNull() ?: emptyList()
            when (itemResult) {
                is RequestResult.Success -> _state.update {
                    it.copy(
                        loading = false,
                        item = itemResult.data,
                        similar = similar,
                        isFav = itemResult.data.inWatchlist,
                    )
                }

                is RequestResult.Error -> _state.update {
                    it.copy(loading = false, error = itemResult.message)
                }
            }
        }
    }

    fun toggleFav() {
        viewModelScope.launch {
            watching.toggleWatchlist(route.itemId).onSuccess { newState ->
                _state.update { it.copy(isFav = newState) }
            }
        }
    }
}

package com.filmax.feature.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.feature.details.navigation.DetailsRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailsUiState(
    val loading: Boolean = true,
    val item: Item? = null,
    val similar: List<Item> = emptyList(),
    val isFav: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class DetailsViewModel @Inject constructor(
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
            try {
                val item = catalog.getItemDetails(route.itemId)
                val similar = catalog.getSimilarItems(route.itemId)
                _state.update {
                    it.copy(
                        loading = false,
                        item = item,
                        similar = similar,
                        isFav = item.inWatchlist
                    )
                }
            } catch (e: Exception) {
                println("kekes: ${e.message}")
                _state.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun toggleFav() {
        viewModelScope.launch {
            val newState = watching.toggleWatchlist(route.itemId)
            _state.update { it.copy(isFav = newState) }
        }
    }
}

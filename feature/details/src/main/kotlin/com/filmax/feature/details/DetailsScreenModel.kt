package com.filmax.feature.details

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.common.onSuccess
import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.core.presentation.BaseScreenModel
import com.filmax.feature.details.navigation.DetailsRoute

class DetailsScreenModel(
    savedStateHandle: SavedStateHandle,
    private val catalog: CatalogRepository,
    private val watching: WatchingRepository,
) : BaseScreenModel<DetailsState, DetailsSideEffect, DetailsEvent>(DetailsState()) {

    private val route = savedStateHandle.toRoute<DetailsRoute>()

    init {
        onFetchData()
    }

    override fun dispatch(event: DetailsEvent) {
        when (event) {
            DetailsEvent.ToggleFav -> toggleFav()
        }
    }

    override fun onFetchData() {
        screenModelScope {
            val itemResult = catalog.getItemDetails(route.itemId)
            val similar = catalog.getSimilarItems(route.itemId).getOrNull() ?: emptyList()
            when (itemResult) {
                is RequestResult.Success -> updateState {
                    it.copy(
                        loading = false,
                        item = itemResult.data,
                        similar = similar,
                        isFav = itemResult.data.inWatchlist,
                    )
                }

                is RequestResult.Error -> updateState {
                    it.copy(loading = false, error = itemResult.message)
                }
            }
        }
    }

    private fun toggleFav() {
        screenModelScope {
            watching.toggleWatchlist(route.itemId).onSuccess { newState ->
                updateState { it.copy(isFav = newState) }
            }
        }
    }
}

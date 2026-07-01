package com.filmax.feature.details.common

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.downloads.DownloadsRepository
import com.filmax.core.domain.downloads.model.DownloadedItem
import com.filmax.core.domain.favorites.FavoritesRepository
import com.filmax.core.domain.favorites.model.toFavoriteItem
import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.core.presentation.BaseScreenModel
import com.filmax.feature.details.common.navigation.DetailsRoute

class DetailsScreenModel(
    savedStateHandle: SavedStateHandle,
    private val catalog: CatalogRepository,
    private val watching: WatchingRepository,
    private val downloads: DownloadsRepository,
    private val favorites: FavoritesRepository,
) : BaseScreenModel<DetailsUiState, DetailsSideEffect, DetailsEvent>(DetailsUiState()) {

    private val route = savedStateHandle.toRoute<DetailsRoute>()

    /** Доменная модель нужна только командам (избранное/загрузка) — в UI-стейт не попадает. */
    private var loadedItem: Item? = null

    init {
        onFetchData()
        observeDownloadState()
        observeFavoriteState()
    }

    override fun dispatch(event: DetailsEvent) {
        when (event) {
            DetailsEvent.ToggleFav -> toggleFav()
            DetailsEvent.ToggleDownload -> toggleDownload()
            is DetailsEvent.SelectTab -> screenModelScope {
                updateState { it.copy(selectedTab = event.tab) }
            }
            is DetailsEvent.SelectSeason -> screenModelScope {
                updateState { it.copy(selectedSeasonIndex = event.index) }
            }
        }
    }

    override fun onFetchData() {
        screenModelScope {
            val itemResult = catalog.getItemDetails(route.itemId)
            val similar = catalog.getSimilarItems(route.itemId).getOrNull().orEmpty()
            when (itemResult) {
                is RequestResult.Success -> {
                    loadedItem = itemResult.data
                    val details = itemResult.data.toDetailsUi(similar)
                    updateState {
                        it.copy(
                            loading = false,
                            details = details,
                            selectedSeasonIndex = details.resumeSeasonIndex,
                        )
                    }
                    // Down-sync: если на сервере фильм уже в watchlist — заносим в локальный кэш.
                    if (itemResult.data.inWatchlist) {
                        favorites.add(itemResult.data.toFavoriteItem())
                    }
                }

                is RequestResult.Error -> {
                    updateState { it.copy(loading = false) }
                    showError(itemResult)
                }
            }
        }
    }

    private fun observeDownloadState() {
        screenModelScope {
            downloads.isDownloaded(route.itemId).collect { downloaded ->
                updateState { it.copy(isDownloaded = downloaded) }
            }
        }
    }

    private fun observeFavoriteState() {
        screenModelScope {
            favorites.isFavorite(route.itemId).collect { favorite ->
                updateState { it.copy(isFav = favorite) }
            }
        }
    }

    private fun toggleFav() {
        val item = loadedItem ?: return
        screenModelScope {
            // Локальный кэш — источник состояния сердечка; сервер синхронизируем best-effort.
            favorites.toggle(item.toFavoriteItem())
            watching.toggleWatchlist(route.itemId)
        }
    }

    private fun toggleDownload() {
        val item = loadedItem ?: return
        screenModelScope {
            if (state.isDownloaded) {
                downloads.remove(item.id)
            } else {
                downloads.add(
                    DownloadedItem(
                        id = item.id,
                        title = item.title,
                        posterSmall = item.posters.medium.ifBlank { item.posters.small },
                        year = item.year,
                        durationMinutes = item.duration.averageMinutes?.toInt() ?: 0,
                    ),
                )
            }
        }
    }
}

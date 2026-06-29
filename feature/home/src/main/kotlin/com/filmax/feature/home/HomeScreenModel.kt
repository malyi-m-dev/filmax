package com.filmax.feature.home

import com.filmax.core.domain.usecase.home.GetHomeFeedUseCase
import com.filmax.core.domain.usecase.watching.ToggleWatchlistUseCase
import com.filmax.core.presentation.BaseScreenModel

class HomeScreenModel(
    private val getHomeFeed: GetHomeFeedUseCase,
    private val toggleWatchlist: ToggleWatchlistUseCase,
) : BaseScreenModel<HomeState, HomeSideEffect, HomeEvent>(HomeState()) {

    init {
        onFetchData()
    }

    override fun dispatch(event: HomeEvent) {
        when (event) {
            HomeEvent.Load -> onFetchData()
            is HomeEvent.ToggleFav -> toggleFav(event.itemId)
        }
    }

    override fun onFetchData() {
        screenModelScope {
            updateState { it.copy(loading = true, error = null) }
            val feed = getHomeFeed()
            updateState { s ->
                s.copy(
                    loading = false,
                    hero = feed.hero,
                    continueWatching = feed.continueWatching,
                    collections = feed.collections,
                    trending = feed.trending,
                    forYou = feed.forYou,
                    error = feed.error,
                )
            }
        }
    }

    private fun toggleFav(itemId: Int) {
        screenModelScope {
            toggleWatchlist(itemId)
            updateState { s ->
                val favs = s.favorites.toMutableSet()
                if (itemId in favs) favs.remove(itemId) else favs.add(itemId)
                s.copy(favorites = favs)
            }
        }
    }
}

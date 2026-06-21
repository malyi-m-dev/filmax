package com.filmax.feature.home

import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.common.firstErrorMessage
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.core.presentation.BaseScreenModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class HomeScreenModel(
    private val catalog: CatalogRepository,
    private val watching: WatchingRepository,
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
            coroutineScope {
                val hotDeferred = async { catalog.getHotItems(ItemType.MOVIE) }
                val trendingDeferred = async { catalog.getItems(ItemType.MOVIE, CatalogSort.VIEWS) }
                val collectionsDeferred = async { catalog.getCollections() }
                val forYouDeferred = async { catalog.getItems(ItemType.SERIES, CatalogSort.RATING) }
                val historyDeferred = async { watching.getHistory() }

                val hot = hotDeferred.await()
                val trending = trendingDeferred.await()
                val collections = collectionsDeferred.await()
                val forYou = forYouDeferred.await()
                val history = historyDeferred.await()

                updateState { s ->
                    s.copy(
                        loading = false,
                        hero = hot.getOrNull()?.items?.firstOrNull(),
                        continueWatching = history.getOrNull()?.take(5) ?: emptyList(),
                        collections = collections.getOrNull()?.take(5) ?: emptyList(),
                        trending = trending.getOrNull()?.items?.take(10) ?: emptyList(),
                        forYou = forYou.getOrNull()?.items?.take(10) ?: emptyList(),
                        error = firstErrorMessage(hot, trending, collections, forYou),
                    )
                }
            }
        }
    }

    private fun toggleFav(itemId: Int) {
        screenModelScope {
            watching.toggleWatchlist(itemId)
            updateState { s ->
                val favs = s.favorites.toMutableSet()
                if (itemId in favs) favs.remove(itemId) else favs.add(itemId)
                s.copy(favorites = favs)
            }
        }
    }
}

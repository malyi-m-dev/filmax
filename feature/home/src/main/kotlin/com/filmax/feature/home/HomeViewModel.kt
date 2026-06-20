package com.filmax.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.common.firstErrorMessage
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.watching.WatchingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val catalog: CatalogRepository,
    private val watching: WatchingRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
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

                _state.update { s ->
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

    fun toggleFav(itemId: Int) {
        viewModelScope.launch {
            watching.toggleWatchlist(itemId)
            _state.update { s ->
                val favs = s.favorites.toMutableSet()
                if (itemId in favs) favs.remove(itemId) else favs.add(itemId)
                s.copy(favorites = favs)
            }
        }
    }
}

package com.filmax.feature.home

import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.watching.model.WatchHistory

data class HomeUiState(
    val loading: Boolean = true,
    val hero: Item? = null,
    val continueWatching: List<WatchHistory> = emptyList(),
    val collections: List<Collection> = emptyList(),
    val trending: List<Item> = emptyList(),
    val forYou: List<Item> = emptyList(),
    val favorites: Set<Int> = emptySet(),
    val error: String? = null,
)

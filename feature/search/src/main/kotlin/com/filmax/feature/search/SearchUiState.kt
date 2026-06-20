package com.filmax.feature.search

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType

data class SearchUiState(
    val query: String = "",
    val filter: ItemType? = null,
    val results: List<Item> = emptyList(),
    val recentQueries: List<String> = emptyList(),
    val trendingQueries: List<String> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)

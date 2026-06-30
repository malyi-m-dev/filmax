package com.filmax.feature.search.common

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType

data class SearchState(
    val query: String = "",
    val filter: ItemType? = null,
    val results: List<Item> = emptyList(),
    val recentQueries: List<String> = emptyList(),
    val trendingQueries: List<String> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)

sealed interface SearchEvent {
    data class QueryChange(val query: String) : SearchEvent
    data class FilterChange(val filter: ItemType?) : SearchEvent

    /** Тап по подсказке (тренды/недавние): подставить запрос и сразу искать. */
    data class SubmitQuery(val query: String) : SearchEvent
    data object ClearRecent : SearchEvent
}

sealed interface SearchSideEffect

package com.filmax.feature.collections.common

import com.filmax.core.domain.catalog.model.Collection

data class CollectionsState(
    val collections: List<Collection> = emptyList(),
    val query: String = "",
    val loading: Boolean = true,
    val error: String? = null,
) {
    val filtered: List<Collection>
        get() = if (query.isBlank()) {
            collections
        } else {
            collections.filter { collection ->
                collection.title.contains(query, ignoreCase = true) ||
                    collection.description?.contains(query, ignoreCase = true) == true
            }
        }
}

sealed interface CollectionsEvent {
    data class QueryChange(val query: String) : CollectionsEvent
}

sealed interface CollectionsSideEffect

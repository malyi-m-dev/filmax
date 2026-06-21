package com.filmax.feature.collections

import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.Item

data class CollectionsUiState(
    val collections: List<Collection> = emptyList(),
    val query: String = "",
    val selectedCollection: Collection? = null,
    val collectionItems: List<Item> = emptyList(),
    val loading: Boolean = true,
    val loadingItems: Boolean = false,
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

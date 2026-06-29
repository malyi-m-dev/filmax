package com.filmax.feature.collections

import com.filmax.core.domain.catalog.model.Item

data class CollectionDetailState(
    val loading: Boolean = true,
    val items: List<Item> = emptyList(),
    val error: String? = null,
)

sealed interface CollectionDetailEvent

sealed interface CollectionDetailSideEffect

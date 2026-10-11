package com.filmax.feature.collections.common

import com.filmax.core.domain.catalog.model.Item

data class CollectionDetailState(
    val loading: Boolean = true,
    val items: List<Item> = emptyList(),
    val error: String? = null,
    /** Последняя загруженная страница (1-based). */
    val page: Int = 0,
    val hasNextPage: Boolean = false,
    val loadingMore: Boolean = false,
)

sealed interface CollectionDetailEvent {

    /** Хвост сетки на экране: подгрузить следующую страницу подборки. */
    data object LoadMore : CollectionDetailEvent
}

sealed interface CollectionDetailSideEffect

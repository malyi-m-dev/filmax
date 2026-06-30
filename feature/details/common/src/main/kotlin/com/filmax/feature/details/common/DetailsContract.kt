package com.filmax.feature.details.common

import com.filmax.core.domain.catalog.model.Item

data class DetailsState(
    val loading: Boolean = true,
    val item: Item? = null,
    val similar: List<Item> = emptyList(),
    val isFav: Boolean = false,
    val isDownloaded: Boolean = false,
    val error: String? = null,
)

sealed interface DetailsEvent {
    data object ToggleFav : DetailsEvent
    data object ToggleDownload : DetailsEvent
}

sealed interface DetailsSideEffect

package com.filmax.feature.player

import com.filmax.core.domain.catalog.model.Item

data class PlayerState(
    val loading: Boolean = true,
    val item: Item? = null,
    val streamUrl: String? = null,
    val error: String? = null,
)

sealed interface PlayerEvent {
    data class SaveProgress(val positionMs: Long) : PlayerEvent
}

sealed interface PlayerSideEffect

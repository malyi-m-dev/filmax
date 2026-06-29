package com.filmax.feature.player

import com.filmax.core.domain.catalog.model.Item

/** Доступное качество потока (метка + ссылка), приходит с бэкенда. */
data class StreamQuality(val label: String, val url: String)

data class PlayerState(
    val loading: Boolean = true,
    val item: Item? = null,
    val streamUrl: String? = null,
    val qualities: List<StreamQuality> = emptyList(),
    val currentQuality: String? = null,
    val error: String? = null,
)

sealed interface PlayerEvent {
    data class SaveProgress(val positionMs: Long) : PlayerEvent
    data class SelectQuality(val label: String) : PlayerEvent
}

sealed interface PlayerSideEffect

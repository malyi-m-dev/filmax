package com.filmax.feature.player.common

/** Трейлер: временный HLS-адрес и название тайтла над кадром — оба из маршрута, экран их не читает. */
data class TrailerState(val url: String = "", val title: String = "")

sealed interface TrailerEvent {
    data object Back : TrailerEvent
}

sealed interface TrailerSideEffect

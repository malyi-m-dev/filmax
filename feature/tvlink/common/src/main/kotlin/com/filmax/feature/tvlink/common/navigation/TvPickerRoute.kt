package com.filmax.feature.tvlink.common.navigation

import kotlinx.serialization.Serializable

/**
 * Выбор телевизора. Аргументы — что запустить на нём, как у `PlayerRoute`: [videoId] уникален только
 * в сезоне [season], [startSeconds] — позиция телефона (-1 — с прогресса kino.pub). [itemId] = -1 —
 * запускать нечего: телевизор выбирают, чтобы открыть пульт.
 */
@Serializable
data class TvPickerRoute(
    val itemId: Int = -1,
    val videoId: Int = -1,
    val season: Int = -1,
    val startSeconds: Int = -1,
)

/** Пульт телевизора — экран телефона. */
@Serializable
data object RemoteRoute

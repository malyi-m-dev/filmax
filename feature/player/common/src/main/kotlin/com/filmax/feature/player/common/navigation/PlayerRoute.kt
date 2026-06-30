package com.filmax.feature.player.common.navigation

import kotlinx.serialization.Serializable

/**
 * Маршрут плеера. Живёт в слое логики — его читает PlayerScreenModel через SavedStateHandle.
 * [videoId] — конкретный эпизод сериала; `-1` (по умолчанию) = первый/единственный трек (фильм).
 */
@Serializable
data class PlayerRoute(val itemId: Int, val videoId: Int = -1)

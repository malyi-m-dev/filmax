package com.filmax.feature.player.navigation

import kotlinx.serialization.Serializable

/** Маршрут плеера. Живёт в слое логики — его читает PlayerScreenModel через SavedStateHandle. */
@Serializable
data class PlayerRoute(val itemId: Int)

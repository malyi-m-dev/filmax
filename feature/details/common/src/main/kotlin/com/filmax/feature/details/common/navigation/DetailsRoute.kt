package com.filmax.feature.details.common.navigation

import kotlinx.serialization.Serializable

/** Маршрут экрана деталей. Живёт в слое логики — его читает DetailsScreenModel через SavedStateHandle. */
@Serializable
data class DetailsRoute(val itemId: Int)

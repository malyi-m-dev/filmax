package com.filmax.feature.search.common.navigation

import kotlinx.serialization.Serializable

/**
 * Маршрут «Фильмографии». Живёт в слое логики — его читает FilmographyScreenModel через
 * SavedStateHandle. [isDirector] выбирает источник: работы режиссёра или роли актёра.
 */
@Serializable
data class FilmographyRoute(val name: String, val isDirector: Boolean)

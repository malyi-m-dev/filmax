package com.filmax.feature.search.common

import com.filmax.core.domain.catalog.model.Item

/**
 * Состояние экрана «Фильмография». [heading] — имя человека для заголовка: вью не читает
 * маршрут, имя приезжает в состоянии уже готовым, поэтому заголовок виден до ответа сети.
 */
data class FilmographyState(
    val loading: Boolean = true,
    val heading: String = "",
    val items: List<Item> = emptyList(),
    val error: String? = null,
)

/** Retry — повтор загрузки после сбоя сети. */
sealed interface FilmographyEvent {
    data object Retry : FilmographyEvent
}

sealed interface FilmographySideEffect

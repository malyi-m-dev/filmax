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

sealed interface FilmographyEvent {
    /** Повтор загрузки после сбоя сети. */
    data object Retry : FilmographyEvent

    /** Работа из сетки — в детали. */
    data class OpenItem(val itemId: Int) : FilmographyEvent
    data object Back : FilmographyEvent
}

/** Одноразовых эффектов у фильмографии нет: переходы модель отдаёт навигатору сама. */
sealed interface FilmographySideEffect

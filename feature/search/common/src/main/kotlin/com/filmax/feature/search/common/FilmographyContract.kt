package com.filmax.feature.search.common

import com.filmax.core.presentation.cards.TitleCard

/**
 * Состояние экрана «Фильмография». [heading] — имя человека для заголовка: вью не читает
 * маршрут, имя приезжает в состоянии уже готовым, поэтому заголовок виден до ответа сети.
 */
data class FilmographyState(
    val loading: Boolean = true,
    val heading: String = "",
    val items: List<TitleCard> = emptyList(),
    val error: String? = null,
) {
    /** Пустая сетка — по сбою сети или потому, что работ нет: причины разные, и подпись тоже. */
    val emptyTitle: String
        get() = if (error == null) "Ничего не найдено" else "Не удалось загрузить"
}

/** Retry — повтор загрузки после сбоя сети. */
sealed interface FilmographyEvent {
    data object Retry : FilmographyEvent
}

sealed interface FilmographySideEffect

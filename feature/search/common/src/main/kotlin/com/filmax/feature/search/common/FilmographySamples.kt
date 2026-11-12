// Неизменные образцы «Фильмографии» — для превью телефона и ТВ.
package com.filmax.feature.search.common

import com.filmax.core.presentation.cards.sampleTitleCard

object FilmographySamples {

    private val works = listOf(
        sampleTitleCard(1, "Дом на краю карты", rating = "8.1", ratingHigh = true),
        sampleTitleCard(2, "Северный ветер", rating = "7.4", typeAndYear = "Сериал · 2023"),
        sampleTitleCard(3, "Код доступа", rating = "6.9", typeAndYear = "Фильм · 2021"),
        sampleTitleCard(4, "Последний рейс", typeAndYear = "Фильм · 2019"),
    )

    val loaded = FilmographyState(loading = false, heading = "Анна Северова", items = works)
    val empty = FilmographyState(loading = false, heading = "Анна Северова")
    val failed = empty.copy(error = "Нет сети")
    val loading = FilmographyState(heading = "Анна Северова")

    val all: List<FilmographyState> = listOf(loaded, empty, failed, loading)
}

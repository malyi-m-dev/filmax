package com.filmax.core.ui.components

import com.filmax.core.domain.catalog.model.Duration
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemRating
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.Posters

/**
 * Образцы данных для предпросмотра компонентов в @Preview и в каталоге дизайн-системы.
 * Постеры пустые — компоненты показывают градиентный плейсхолдер.
 */
object FilmaxPreviewSamples {
    val movie = Item(
        id = 1,
        title = "Дюна: Часть вторая",
        type = ItemType.MOVIE,
        year = 2024,
        plot = "Пол Атрейдес объединяется с Чани и фрименами, ведя войну против дома Харконненов.",
        director = "Дени Вильнёв",
        cast = "Тимоти Шаламе, Зендея, Ребекка Фергюсон",
        country = "США",
        genres = listOf(Genre(1, "Фантастика"), Genre(2, "Драма")),
        rating = ItemRating(filmax = 86, filmaxPercentage = "86%", imdb = "8.6", kinopoisk = "8.3"),
        posters = Posters(small = "", medium = "", big = "", wide = null),
        duration = Duration(averageMinutes = 166.0, totalMinutes = 166),
        tracklist = emptyList(),
        trailer = null,
        inWatchlist = false,
        finished = false,
    )
}

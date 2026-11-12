// Карточка тайтла для превью: образцы состояний экранов (XxxSamples) собирают постеры одной
// функцией, а не каждый своим конструктором на десяток полей.
package com.filmax.core.presentation.cards

/**
 * Карточка-образец без картинок: превью рисует плейсхолдер постера. [typeAndYear] — «Фильм · 2022»;
 * год из него же идёт в [TitleCard.yearAndLength], а с ним [length], если задана.
 */
@Suppress("LongParameterList") // Образец задают именованными аргументами, у каждого — значение по умолчанию.
fun sampleTitleCard(
    id: Int,
    title: String,
    rating: String? = null,
    ratingHigh: Boolean = false,
    typeAndYear: String = "Фильм · 2022",
    length: String? = null,
    genre: String? = null,
): TitleCard {
    val movie = typeAndYear.startsWith("Фильм")
    val year = typeAndYear.substringAfter(" · ")
    return TitleCard(
        id = id,
        title = title,
        posterUrl = "",
        widePosterUrl = "",
        thumbUrl = "",
        rating = rating,
        ratingHigh = ratingHigh,
        typeAndYear = typeAndYear,
        yearAndLength = listOfNotNull(year, length).joinToString(" · "),
        genre = genre,
        freshBadge = if (movie) "Премьера" else "Новинка",
    )
}

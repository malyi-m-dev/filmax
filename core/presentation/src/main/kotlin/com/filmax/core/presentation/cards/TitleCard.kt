package com.filmax.core.presentation.cards

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.presentation.format.durationLabel
import com.filmax.core.presentation.format.isHighRating
import com.filmax.core.presentation.format.posterMeta
import com.filmax.core.presentation.format.ratingLabel
import com.filmax.core.presentation.format.typeLabel

/**
 * Постер тайтла с готовыми подписями — одна модель на сетки и ряды телефона и ТВ. Подписи
 * собирает модель экрана: вёрстке остаётся только нарисовать карточку.
 */
data class TitleCard(
    val id: Int,
    val title: String,
    /** Средний постер, иначе большой, иначе маленький. */
    val posterUrl: String,
    /** Широкий кадр 16:9, иначе большой постер — для широких карточек «Новинок». */
    val widePosterUrl: String,
    /** Главная оценка источника одним знаком; null — оценки нет. */
    val rating: String?,
    /** Высокая оценка — пилюля выделяется вторым акцентом. */
    val ratingHigh: Boolean,
    /** «Фильм · 2021». */
    val typeAndYear: String?,
    /** «2021 · 1 ч 52 мин» — у фильма с длительностью; у сериала только год. */
    val yearAndLength: String?,
    /** Плашка новинки: у фильма — «Премьера», у сериала и шоу — «Новинка» (новая серия). */
    val freshBadge: String,
)

fun Item.toTitleCard(): TitleCard {
    val minutes = duration.averageMinutes?.toInt()?.takeIf { it > 0 && !type.episodic }
    val year = posterMeta(type = null, year = year)
    return TitleCard(
        id = id,
        title = title,
        posterUrl = posters.medium.ifBlank { posters.big }.ifBlank { posters.small },
        widePosterUrl = posters.wide?.takeIf { it.isNotBlank() } ?: posters.big,
        rating = ratingLabel(rating.primary),
        ratingHigh = isHighRating(rating.primary),
        typeAndYear = posterMeta(typeLabel(type), this.year),
        yearAndLength = listOfNotNull(year, minutes?.let(::durationLabel)).joinToString(" · ").ifBlank { null },
        freshBadge = if (type == ItemType.MOVIE) "Премьера" else "Новинка",
    )
}

// Герой Главной с готовыми подписями: мету телефон и ТВ собирают по-разному (сколько жанров
// помещается в строку), поэтому в карточке обе. Считает модель, вёрстка только рисует.
package com.filmax.feature.home.common

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.presentation.format.durationLabel
import com.filmax.core.presentation.format.ratingLabel

/** Больше трёх жанров мета-строка ТВ не вмещает по ширине текстового блока. */
private const val MAX_TV_GENRES = 3

/** Тайтл карусели hero. */
data class HeroCard(
    val id: Int,
    val title: String,
    val plot: String,
    /** Кадр телефона: большой постер, иначе широкий. */
    val posterUrl: String,
    /** Фон ТВ: широкий кадр, иначе большой постер. */
    val backdropUrl: String,
    /** Главная оценка одним знаком; null — оценки нет. */
    val rating: String?,
    /** «Фантастика · 2024 · 2 ч 46 мин» — с первым жанром: телефон. */
    val meta: List<String>,
    /** То же с тремя жанрами: ТВ, где строка шире. */
    val wideMeta: List<String>,
)

fun Item.toHeroCard(): HeroCard {
    val year = year.takeIf { it > 0 }?.toString()
    val length = duration.averageMinutes?.toInt()?.takeIf { it > 0 }?.let(::durationLabel)
    val wideGenres = genres.take(MAX_TV_GENRES).joinToString(" · ") { it.title }.ifBlank { null }
    return HeroCard(
        id = id,
        title = title,
        plot = plot,
        posterUrl = posters.big.ifBlank { posters.wide.orEmpty() },
        backdropUrl = posters.wide?.takeIf { it.isNotBlank() } ?: posters.big,
        rating = ratingLabel(rating.primary),
        meta = listOfNotNull(genres.firstOrNull()?.title, year, length),
        wideMeta = listOfNotNull(wideGenres, year, length),
    )
}

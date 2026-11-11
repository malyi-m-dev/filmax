// Подписи оценок для экранов — одни на телефоне и ТВ, в деталях и в фильтрах каталога.
package com.filmax.core.presentation.format

import com.filmax.core.domain.catalog.model.RatingSource

/** Полное название — бейдж оценки в деталях, строка фильтра. */
val RatingSource.title: String
    get() = when (this) {
        RatingSource.KINOPOISK -> "КиноПоиск"
        RatingSource.IMDB -> "IMDb"
        RatingSource.COMMUNITY -> "Рейтинг"
    }

/** Короткое — мета под названием, чип применённого фильтра. */
val RatingSource.shortTitle: String
    get() = when (this) {
        RatingSource.KINOPOISK -> "КП"
        RatingSource.IMDB -> "IMDb"
        RatingSource.COMMUNITY -> "Рейтинг"
    }

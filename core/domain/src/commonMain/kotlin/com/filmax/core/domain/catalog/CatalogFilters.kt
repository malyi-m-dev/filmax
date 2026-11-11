package com.filmax.core.domain.catalog

import com.filmax.core.domain.catalog.model.RatingSource

/**
 * Диапазонные фильтры каталога (экран «Каталог»). Пустое значение поля — «не фильтровать».
 *
 * Домен хранит намерение, а не формат запроса: условия kino.pub (`year>=2020`,
 * `kinopoisk_rating>=7`) и параметры домашнего сервера собирают их data-слои.
 *
 * [minRatings] — порог по каждой оценке («КП от 7», «IMDb от 8»). Какие оценки есть у источника,
 * говорят его возможности (`SourceCapabilities.ratings`): экран предлагает только их.
 *
 * [onlyFinished] тернарный: null — «неважно», true — только завершённые сериалы, false — только
 * продолжающиеся. Мобильный тумблер использует пару null/true, домен допускает и false.
 */
data class CatalogFilters(
    val yearFrom: Int? = null,
    val yearTo: Int? = null,
    val minRatings: Map<RatingSource, Int> = emptyMap(),
    val countryId: Int? = null,
    val only4k: Boolean = false,
    val onlyFinished: Boolean? = null,
) {
    /** Сколько фильтров реально задано — для бейджа-счётчика на иконке «Фильтры». */
    val activeCount: Int
        get() = listOf(
            yearFrom != null || yearTo != null,
            countryId != null,
            only4k,
            onlyFinished != null,
        ).count { active -> active } + minRatings.size

    /** Порог по оценке [source]; null — не задан. */
    fun minRating(source: RatingSource): Int? = minRatings[source]

    /** Копия с порогом [value] по оценке [source]; null — снять порог. */
    fun withMinRating(source: RatingSource, value: Int?): CatalogFilters =
        copy(minRatings = if (value == null) minRatings - source else minRatings + (source to value))
}

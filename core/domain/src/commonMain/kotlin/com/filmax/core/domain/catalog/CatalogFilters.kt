package com.filmax.core.domain.catalog

/**
 * Диапазонные фильтры каталога (экран «Каталог»). Пустое значение поля — «не фильтровать».
 *
 * Домен хранит намерение, а не формат запроса: строки условий kino.pub (`year>=2020`,
 * `kinopoisk_rating>=7`) собирает data-слой. Так фильтры остаются переиспользуемыми на iOS,
 * где своя сборка запроса.
 *
 * [onlyFinished] тернарный: null — «неважно», true — только завершённые сериалы, false — только
 * продолжающиеся. Мобильный тумблер использует пару null/true, домен допускает и false.
 */
data class CatalogFilters(
    val yearFrom: Int? = null,
    val yearTo: Int? = null,
    val kpRatingFrom: Int? = null,
    val imdbRatingFrom: Int? = null,
    val countryId: Int? = null,
    val only4k: Boolean = false,
    val onlyFinished: Boolean? = null,
) {
    /** Сколько фильтров реально задано — для бейджа-счётчика на иконке «Фильтры». */
    val activeCount: Int
        get() = listOf(
            yearFrom != null || yearTo != null,
            kpRatingFrom != null,
            imdbRatingFrom != null,
            countryId != null,
            only4k,
            onlyFinished != null,
        ).count { active -> active }
}

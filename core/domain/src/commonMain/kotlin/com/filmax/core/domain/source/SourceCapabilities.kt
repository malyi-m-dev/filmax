package com.filmax.core.domain.source

import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.model.RatingSource

/**
 * Что умеет источник. Экраны показывают только доступное, а не ловят ошибку после нажатия: у
 * домашнего сервера нет папок, оценки КиноПоиска и фильтра по стране.
 */
data class SourceCapabilities(
    /** Оценки тайтлов, по которым можно фильтровать; первая — главная оценка источника. */
    val ratings: List<RatingSource>,
    /** Поля сортировки каталога. */
    val sorts: List<CatalogSort>,
    /** Фильтр каталога по стране производства. */
    val countries: Boolean,
    /** Папки-закладки: создать, добавить, открыть. */
    val folders: Boolean,
    /** Доступ по подписке: Профиль показывает её срок, плеер — плашку без неё. */
    val subscription: Boolean,
)

/** Возможности источника. Домен их знает: источников два, и различие — часть их описания. */
val ContentSource.capabilities: SourceCapabilities
    get() = when (this) {
        ContentSource.KINOPUB -> SourceCapabilities(
            ratings = listOf(RatingSource.KINOPOISK, RatingSource.IMDB),
            sorts = CatalogSort.entries,
            countries = true,
            folders = true,
            subscription = true,
        )
        ContentSource.JELLYFIN -> SourceCapabilities(
            ratings = listOf(RatingSource.COMMUNITY),
            sorts = listOf(
                CatalogSort.UPDATED,
                CatalogSort.CREATED,
                CatalogSort.RATING,
                CatalogSort.VIEWS,
                CatalogSort.YEAR,
            ),
            countries = false,
            folders = false,
            subscription = false,
        )
    }

package com.filmax.core.domain.watchlist.model

import com.filmax.core.domain.catalog.model.Item

/** Тайтл в «Буду смотреть» — ровно то, что нужно карточке «Моего» и её сортировкам. */
data class WatchlistItem(
    val id: Int,
    val title: String,
    val posterSmall: String,
    val year: Int,
    val durationMinutes: Int,
    /**
     * Главная оценка источника ([com.filmax.core.domain.catalog.model.ItemRating.primary]). Нужна
     * пилюле на постере и сортировке «Буду смотреть» по рейтингу; null — оценки нет или запись
     * сохранена до того, как оценку стали хранить.
     */
    val rating: Double? = null,
)

/** Лёгкая запись списка из полного [Item]. */
fun Item.toWatchlistItem() = WatchlistItem(
    id = id,
    title = title,
    posterSmall = posters.medium.ifBlank { posters.small },
    year = year,
    durationMinutes = duration.averageMinutes?.toInt() ?: 0,
    rating = rating.primary,
)

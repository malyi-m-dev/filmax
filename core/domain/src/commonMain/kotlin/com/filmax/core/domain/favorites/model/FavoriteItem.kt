package com.filmax.core.domain.favorites.model

import com.filmax.core.domain.catalog.model.Item

/** Метаданные избранного фильма для локального хранения и отображения. */
data class FavoriteItem(
    val id: Int,
    val title: String,
    val posterSmall: String,
    val year: Int,
    val durationMinutes: Int,
    /**
     * Оценка Кинопоиска строкой, как её отдаёт kino.pub («8.3»). Нужна пилюле на постере и
     * сортировке «Буду смотреть» по рейтингу; null — оценки нет или запись сохранена до того,
     * как оценку стали хранить.
     */
    val rating: String? = null,
)

/** Конвертация полного [Item] в лёгкую запись избранного. */
fun Item.toFavoriteItem() = FavoriteItem(
    id = id,
    title = title,
    posterSmall = posters.medium.ifBlank { posters.small },
    year = year,
    durationMinutes = duration.averageMinutes?.toInt() ?: 0,
    rating = rating.kinopoisk,
)

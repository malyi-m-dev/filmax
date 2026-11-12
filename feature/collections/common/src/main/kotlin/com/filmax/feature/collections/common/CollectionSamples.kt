// Неизменные образцы открытой подборки — для превью телефона и ТВ.
package com.filmax.feature.collections.common

import com.filmax.core.presentation.cards.TitleCard

object CollectionSamples {

    private val items = listOf(
        card(1, "Дом на краю карты", rating = "8.1", typeAndYear = "Фильм · 2022", high = true),
        card(2, "Северный ветер", rating = "7.4", typeAndYear = "Сериал · 2023"),
        card(3, "Полярный круг", rating = null, typeAndYear = "Фильм · 2019"),
        card(4, "Код доступа", rating = "6.9", typeAndYear = "Фильм · 2021"),
        card(5, "Тихие вечера", rating = "8.4", typeAndYear = "Сериал · 2020", high = true),
        card(6, "Последний рейс", rating = "7.7", typeAndYear = "Фильм · 2024"),
    )

    private fun card(id: Int, title: String, rating: String?, typeAndYear: String, high: Boolean = false) = TitleCard(
        id = id,
        title = title,
        posterUrl = "",
        widePosterUrl = "",
        rating = rating,
        ratingHigh = high,
        typeAndYear = typeAndYear,
        yearAndLength = typeAndYear.substringAfter(" · "),
        freshBadge = "Новинка",
    )

    /** Подборка в одну страницу: число тайтлов точное. */
    val loaded = CollectionDetailState(
        collectionId = 102,
        title = "Сериалы на один вечер",
        loading = false,
        items = items,
        page = 1,
    )

    /** Впереди ещё страницы: числа в шапке нет. */
    val firstPage = loaded.copy(collectionId = 101, title = "Зрители рекомендуют", hasNextPage = true)

    val empty = loaded.copy(items = emptyList())
    val loading = CollectionDetailState(collectionId = 102, title = "Сериалы на один вечер")

    val all: List<CollectionDetailState> = listOf(loaded, firstPage, empty, loading)
}

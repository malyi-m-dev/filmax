// Неизменные образцы открытой подборки — для превью телефона и ТВ.
package com.filmax.feature.collections.common

import com.filmax.core.presentation.cards.sampleTitleCard

object CollectionSamples {

    private val items = listOf(
        sampleTitleCard(1, "Дом на краю карты", rating = "8.1", ratingHigh = true),
        sampleTitleCard(2, "Северный ветер", rating = "7.4", typeAndYear = "Сериал · 2023"),
        sampleTitleCard(3, "Полярный круг", typeAndYear = "Фильм · 2019"),
        sampleTitleCard(4, "Код доступа", rating = "6.9", typeAndYear = "Фильм · 2021"),
        sampleTitleCard(5, "Тихие вечера", rating = "8.4", ratingHigh = true, typeAndYear = "Сериал · 2020"),
        sampleTitleCard(6, "Последний рейс", rating = "7.7", typeAndYear = "Фильм · 2024"),
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

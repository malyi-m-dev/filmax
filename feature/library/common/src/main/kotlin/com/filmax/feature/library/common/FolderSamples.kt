// Неизменные образцы открытой папки — для превью телефона и ТВ.
package com.filmax.feature.library.common

import com.filmax.core.domain.user.model.UserFolder
import com.filmax.core.presentation.cards.TitleCard

object FolderSamples {

    private val folder = UserFolder(id = 2, title = "Пересмотреть с семьёй", count = 3, updatedAt = null, tint = 1)

    private val items = listOf(
        card(1, "Дом на краю карты", rating = "8.1", typeAndYear = "Фильм · 2022", yearAndLength = "2022 · 1 ч 52 мин"),
        card(2, "Северный ветер", rating = "7.4", typeAndYear = "Сериал · 2023", yearAndLength = "2023"),
        card(3, "Полярный круг", rating = null, typeAndYear = "Концерт · 2019", yearAndLength = "2019 · 1 ч 35 мин"),
    )

    private fun card(id: Int, title: String, rating: String?, typeAndYear: String, yearAndLength: String) = TitleCard(
        id = id,
        title = title,
        posterUrl = "",
        widePosterUrl = "",
        rating = rating,
        ratingHigh = rating == "8.1",
        typeAndYear = typeAndYear,
        yearAndLength = yearAndLength,
        freshBadge = "Новинка",
    )

    val loaded = FolderState(loading = false, folder = folder, items = items, page = 1, endReached = true)
    val empty = FolderState(loading = false, folder = folder.copy(count = 0), page = 1, endReached = true)
    val loading = FolderState()

    val all: List<FolderState> = listOf(loaded, empty, loading)
}

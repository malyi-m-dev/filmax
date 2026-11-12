// Неизменные образцы открытой папки — для превью телефона и ТВ.
package com.filmax.feature.library.common

import com.filmax.core.domain.user.model.UserFolder
import com.filmax.core.presentation.cards.sampleTitleCard

object FolderSamples {

    private val folder = UserFolder(id = 2, title = "Пересмотреть с семьёй", count = 3, updatedAt = null, tint = 1)

    private val items = listOf(
        sampleTitleCard(1, "Дом на краю карты", rating = "8.1", ratingHigh = true, length = "1 ч 52 мин"),
        sampleTitleCard(2, "Северный ветер", rating = "7.4", typeAndYear = "Сериал · 2023"),
        sampleTitleCard(3, "Полярный круг", typeAndYear = "Концерт · 2019", length = "1 ч 35 мин"),
    )

    val loaded = FolderState(loading = false, folder = folder, items = items, page = 1, endReached = true)
    val empty = FolderState(loading = false, folder = folder.copy(count = 0), page = 1, endReached = true)
    val loading = FolderState()

    val all: List<FolderState> = listOf(loaded, empty, loading)
}

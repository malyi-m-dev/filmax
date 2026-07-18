package com.filmax.feature.details.common

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.person.CastMember

data class DetailsState(
    val loading: Boolean = true,
    val item: Item? = null,
    val similar: List<Item> = emptyList(),
    /**
     * Актёры с фото (TMDB) — украшение поверх строки имён от kino.pub. Пустой список, когда фото
     * недоступны (нет ключа TMDB, нет совпадения по IMDb, сбой): экран падает на строку `item.cast`.
     */
    val cast: List<CastMember> = emptyList(),
    val isFav: Boolean = false,
    val isDownloaded: Boolean = false,
    val error: String? = null,
)

sealed interface DetailsEvent {
    data object ToggleFav : DetailsEvent
    data object ToggleDownload : DetailsEvent
}

sealed interface DetailsSideEffect

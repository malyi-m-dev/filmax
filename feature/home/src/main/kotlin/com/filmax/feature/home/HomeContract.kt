package com.filmax.feature.home

import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.watching.model.WatchHistory

data class HomeState(
    val loading: Boolean = true,
    val hero: Item? = null,
    val continueWatching: List<WatchHistory> = emptyList(),
    val collections: List<Collection> = emptyList(),
    val trending: List<Item> = emptyList(),
    val forYou: List<Item> = emptyList(),
    val error: String? = null,
)

sealed interface HomeEvent {
    data object Load : HomeEvent
}

/** Экран пока не порождает одноразовых эффектов — навигация открытия айтема идёт колбэком из Screen. */
sealed interface HomeSideEffect

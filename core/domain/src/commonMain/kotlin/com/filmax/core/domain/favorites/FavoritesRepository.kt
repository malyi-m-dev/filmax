package com.filmax.core.domain.favorites

import com.filmax.core.domain.favorites.model.FavoriteItem
import kotlinx.coroutines.flow.Flow

/**
 * Локальный кэш избранного (watchlist). Сервер отдаёт лишь тоггл и флаг `inWatchlist`
 * на самом фильме, но НЕ список — поэтому список держим локально и используем как
 * источник для Библиотеки; синхронизация с сервером идёт на тоггле и при импорте флага.
 */
interface FavoritesRepository {
    val favorites: Flow<List<FavoriteItem>>
    val favoriteIds: Flow<Set<Int>>

    fun isFavorite(id: Int): Flow<Boolean>

    /** Переключает локальное состояние, возвращает новое (true — в избранном). */
    suspend fun toggle(item: FavoriteItem): Boolean

    suspend fun add(item: FavoriteItem)

    suspend fun remove(id: Int)
}

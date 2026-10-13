package com.filmax.core.domain.usecase.watching

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.favorites.FavoritesRepository
import com.filmax.core.domain.favorites.model.toFavoriteItem
import com.filmax.core.domain.watching.WatchingRepository

/**
 * Тонкие UseCase для действий над просмотром — общий контракт для Android и iOS.
 */

class ToggleWatchlistUseCase(private val repository: WatchingRepository) {
    /** @return новое состояние «в списке» (true — добавлено). */
    suspend operator fun invoke(itemId: Int): RequestResult<Boolean> = repository.toggleWatchlist(itemId)
}

class ToggleWatchedUseCase(private val repository: WatchingRepository) {
    suspend operator fun invoke(itemId: Int): RequestResult<Unit> = repository.toggleWatched(itemId)
}

/**
 * «Буду смотреть» целиком: локальная отметка — источник состояния кнопки и списка «Моё»
 * (сервер списка не отдаёт), серверный флаг синхронизируется вслед, best-effort.
 */
class ToggleWatchLaterUseCase(
    private val favorites: FavoritesRepository,
    private val watching: WatchingRepository,
) {
    /** @return новое состояние: true — тайтл в «Буду смотреть». */
    suspend operator fun invoke(item: Item): Boolean {
        val added = favorites.toggle(item.toFavoriteItem())
        watching.toggleWatchlist(item.id)
        return added
    }
}

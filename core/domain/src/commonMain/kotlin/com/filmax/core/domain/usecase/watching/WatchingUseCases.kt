package com.filmax.core.domain.usecase.watching

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.common.getOrNull
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

/**
 * Убрать тайтл из «Буду смотреть» по одному id — долгое «ОК» в «Моё», где полного тайтла нет.
 *
 * Серверный флаг `in_watchlist` надо снять тоже: иначе первое же открытие деталей вернуло бы
 * тайтл в список (детали досинхронизируют флаг в избранное). Флаг у kino.pub — только тоггл, и
 * если тайтл им отмечен не был, первый вызов его поставит — тогда второй снимает обратно.
 */
class RemoveFromWatchLaterUseCase(
    private val favorites: FavoritesRepository,
    private val watching: WatchingRepository,
) {
    suspend operator fun invoke(itemId: Int) {
        favorites.remove(itemId)
        if (watching.toggleWatchlist(itemId).getOrNull() == true) watching.toggleWatchlist(itemId)
    }
}

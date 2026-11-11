package com.filmax.core.domain.usecase.watching

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.core.domain.watchlist.WatchlistRepository
import com.filmax.core.domain.watchlist.model.toWatchlistItem
import kotlinx.coroutines.flow.first

/**
 * Тонкие UseCase для действий над просмотром — общий контракт для Android и iOS.
 */

class ToggleWatchedUseCase(private val repository: WatchingRepository) {
    suspend operator fun invoke(itemId: Int): RequestResult<Unit> = repository.toggleWatched(itemId)
}

/**
 * Кнопка «Буду смотреть»: отмечен — снять, нет — отметить. Решает по списку репозитория, а не по
 * флагу на тайтле: флаг в деталях мог устареть, а кнопка и «Моё» должны говорить одно.
 */
class ToggleWatchlistUseCase(private val watchlist: WatchlistRepository) {
    /** @return новое состояние: true — тайтл в «Буду смотреть». */
    suspend operator fun invoke(item: Item): Boolean {
        val added = !watchlist.contains(item.id).first()
        if (added) watchlist.add(item.toWatchlistItem()) else watchlist.remove(item.id)
        return added
    }
}

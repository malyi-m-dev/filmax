package com.filmax.core.domain.usecase.watching

import com.filmax.core.domain.common.RequestResult
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

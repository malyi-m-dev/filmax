package com.filmax.data.fake

import com.filmax.core.domain.watchlist.WatchlistRepository
import com.filmax.core.domain.watchlist.model.WatchlistItem
import com.filmax.core.domain.watchlist.model.toWatchlistItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * «Буду смотреть» на фейковом сервере: папка [FakeIds.WATCHLIST_FOLDER] и флаг `in_watchlist`
 * вместе, как их ведёт KinopubWatchlistRepository на живом kino.pub. Список читается из той же
 * папки, что видят «Моё» и выбор папки, поэтому экраны мок-сборки не расходятся.
 *
 * Отметка оптимистичная, как у настоящего репозитория: «сервер» меняется сразу, задержка — только
 * у ответа.
 */
class FakeWatchlistRepository(private val backend: FakeBackend) : WatchlistRepository {

    override val items: Flow<List<WatchlistItem>> = backend.library.watchlistIds.map { ids ->
        // Папка — свежедобавленные первыми, а список «Буду смотреть» — в порядке добавления.
        ids.reversed().mapNotNull { id -> backend.catalog.entry(id)?.item?.toWatchlistItem() }
    }

    override fun contains(itemId: Int): Flow<Boolean> =
        backend.library.watchlistIds.map { ids -> itemId in ids }.distinctUntilChanged()

    override suspend fun add(item: WatchlistItem) {
        backend.library.mark(item.id, marked = true)
        backend.respond { success(Unit) }
    }

    override suspend fun remove(itemId: Int) {
        backend.library.mark(itemId, marked = false)
        backend.respond { success(Unit) }
    }

    override suspend fun adopt(item: WatchlistItem) {
        backend.library.addItem(FakeIds.WATCHLIST_FOLDER, item.id)
    }
}

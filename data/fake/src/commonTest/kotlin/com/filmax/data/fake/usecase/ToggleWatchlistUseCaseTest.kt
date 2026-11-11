package com.filmax.data.fake.usecase

import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.usecase.watching.ToggleWatchlistUseCase
import com.filmax.core.domain.watchlist.model.toWatchlistItem
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeCatalogRepository
import com.filmax.data.fake.FakeIds
import com.filmax.data.fake.FakeLatency
import com.filmax.data.fake.FakeUserRepository
import com.filmax.data.fake.FakeWatchlistRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * «Буду смотреть» у kino.pub — папка и флаг `in_watchlist`. Кнопка меняет оба одинаково, туда и
 * обратно, а отметку, пришедшую с сервера, список подхватывает.
 */
class ToggleWatchlistUseCaseTest {

    private val backend = FakeBackend(latency = FakeLatency.None)
    private val catalog = FakeCatalogRepository(backend)
    private val watchlist = FakeWatchlistRepository(backend)
    private val toggleWatchlist = ToggleWatchlistUseCase(watchlist)

    @Test
    fun `toggle puts the title into the folder and flags it, second toggle takes both back`() = runTest {
        val item = checkNotNull(catalog.getItemDetails(FakeIds.LONG_TITLE_MOVIE).getOrNull())
        assertFalse(item.inWatchlist)

        assertTrue(toggleWatchlist(item))
        assertTrue(watchlist.contains(item.id).first())
        assertEquals(true, serverFlag(item.id))
        assertTrue(item.id in watchlistFolder())

        assertFalse(toggleWatchlist(item))
        assertFalse(watchlist.contains(item.id).first())
        assertEquals(false, serverFlag(item.id))
        assertFalse(item.id in watchlistFolder())
    }

    @Test
    fun `list follows the order titles were added in`() = runTest {
        val first = checkNotNull(catalog.getItemDetails(FakeIds.LONG_TITLE_MOVIE).getOrNull())
        val second = checkNotNull(catalog.getItemDetails(FakeIds.CONCERT).getOrNull())

        toggleWatchlist(first)
        watchlist.adopt(second.toWatchlistItem())

        assertEquals(listOf(first.id, second.id), watchlist.items.first().takeLast(2).map { it.id })
    }

    private suspend fun serverFlag(id: Int): Boolean? = catalog.getItemDetails(id).getOrNull()?.inWatchlist

    private suspend fun watchlistFolder(): List<Int> =
        FakeUserRepository(backend).getBookmarkItems(FakeIds.WATCHLIST_FOLDER, page = 1).getOrNull()
            ?.items.orEmpty().map { it.id }
}

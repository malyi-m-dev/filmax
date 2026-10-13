package com.filmax.data.fake.usecase

import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.usecase.watching.ToggleWatchLaterUseCase
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeCatalogRepository
import com.filmax.data.fake.FakeFavoritesRepository
import com.filmax.data.fake.FakeIds
import com.filmax.data.fake.FakeLatency
import com.filmax.data.fake.FakeWatchingRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * «Буду смотреть» живёт в двух местах: локальный список (из него собирается «Моё») и флаг
 * watchlist на сервере. Переключение должно менять оба одинаково, туда и обратно.
 */
class ToggleWatchLaterUseCaseTest {

    private val backend = FakeBackend(latency = FakeLatency.None)
    private val catalog = FakeCatalogRepository(backend)
    private val favorites = FakeFavoritesRepository()
    private val toggleWatchLater = ToggleWatchLaterUseCase(favorites, FakeWatchingRepository(backend))

    @Test
    fun `toggle adds the title locally and on the server, second toggle removes it`() = runTest {
        val item = checkNotNull(catalog.getItemDetails(FakeIds.LONG_TITLE_MOVIE).getOrNull())
        assertFalse(item.inWatchlist)

        assertTrue(toggleWatchLater(item))
        assertTrue(favorites.isFavorite(item.id).first())
        assertEquals(true, serverWatchlist(item.id))

        assertFalse(toggleWatchLater(item))
        assertFalse(favorites.isFavorite(item.id).first())
        assertEquals(false, serverWatchlist(item.id))
    }

    private suspend fun serverWatchlist(id: Int): Boolean? = catalog.getItemDetails(id).getOrNull()?.inWatchlist
}

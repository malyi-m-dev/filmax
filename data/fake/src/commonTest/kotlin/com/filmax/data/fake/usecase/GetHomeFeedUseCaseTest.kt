package com.filmax.data.fake.usecase

import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.common.LastValueCache
import com.filmax.core.domain.usecase.home.GetHomeFeedUseCase
import com.filmax.core.domain.usecase.home.HomeFeed
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeCatalogRepository
import com.filmax.data.fake.FakeLatency
import com.filmax.data.fake.FakeWatchingRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Лента Главной на фейковом бэкенде — те же запросы kino.pub, что и на живом: карусель hero,
 * новинки фильмов и сериалов вперемешку, кэш последней полной ленты.
 */
class GetHomeFeedUseCaseTest {

    private val backend = FakeBackend(latency = FakeLatency.None)
    private val cache = LastValueCache<HomeFeed>()
    private val getHomeFeed = GetHomeFeedUseCase(
        catalog = FakeCatalogRepository(backend),
        watching = FakeWatchingRepository(backend),
        cache = cache,
    )

    @Test
    fun `hero carousel holds up to four distinct titles and the first is the main one`() = runTest {
        val feed = getHomeFeed()

        assertTrue(feed.heroes.size in 2..HERO_LIMIT, "в карусели ${feed.heroes.size} тайтлов")
        assertEquals(feed.heroes.size, feed.heroes.map { it.id }.distinct().size)
        assertSame(feed.heroes.first(), feed.hero)
    }

    @Test
    fun `fresh row alternates movies and series`() = runTest {
        val types = getHomeFeed().fresh.map { it.type }

        assertTrue(types.size in 2..ROW_LIMIT, "в новинках ${types.size} тайтлов")
        assertEquals(ItemType.MOVIE, types.first())
        // Пока есть и те и другие — подряд два одного типа не встречаются.
        val paired = types.take(2 * minOf(types.count { it == ItemType.MOVIE }, types.count { it != ItemType.MOVIE }))
        paired.zipWithNext().forEach { (current, next) -> assertTrue(current != next, "подряд два $current: $types") }
    }

    @Test
    fun `complete feed is cached and reported as fresh`() = runTest {
        val feed = getHomeFeed()

        assertNull(feed.error)
        assertFalse(feed.fromCache)
        assertTrue(feed.hasContent)
        assertEquals(feed, cache.get())
    }

    private companion object {
        const val HERO_LIMIT = 4
        const val ROW_LIMIT = 20
    }
}

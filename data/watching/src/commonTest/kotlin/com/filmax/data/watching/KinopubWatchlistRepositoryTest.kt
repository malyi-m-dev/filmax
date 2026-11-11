package com.filmax.data.watching

import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.watchlist.model.WatchlistItem
import com.filmax.core.network.networkJson
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeIds
import com.filmax.data.fake.FakeLatency
import com.filmax.data.fake.FakeUserRepository
import com.filmax.data.watching.remote.WatchingApi
import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * «Буду смотреть» kino.pub: папка-закладка на фейковом сервере и флаг `in_watchlist` на подменённом
 * движке. Флаг у kino.pub — только переключатель, поэтому важно, сколько раз его дёрнули.
 */
class KinopubWatchlistRepositoryTest {

    private val backend = FakeBackend(latency = FakeLatency.None)
    private val folders = FakeUserRepository(backend)
    private val settings = MapSettings()
    private val signedIn = MutableStateFlow(true)

    /** Тайтлы с флагом на «сервере» и число переключений флага. */
    private val flagged = mutableSetOf<Int>()
    private var flagToggles = 0

    private val engine = MockEngine { request ->
        val itemId = request.url.parameters["id"]?.toInt() ?: error("togglewatchlist без id")
        flagToggles++
        if (!flagged.remove(itemId)) flagged += itemId
        val watching = if (itemId in flagged) 1 else 0
        respond(
            content = """{"status":200,"watching":$watching}""",
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    }

    private val api = WatchingApi(HttpClient(engine) { install(ContentNegotiation) { json(networkJson) } })

    @Test
    fun `signing in reads the watchlist folder into the list`() = runTest {
        val repository = repository(backgroundScope)
        runCurrent()

        assertEquals(folderItems().toSet(), repository.items.first().map { it.id }.toSet())
        assertTrue(repository.contains(FakeIds.HERO_MOVIE).first())
    }

    @Test
    fun `add puts the title into the folder and flags it, remove takes both back`() = runTest {
        val repository = repository(backgroundScope)
        runCurrent()

        repository.add(title(FakeIds.LONG_TITLE_MOVIE))
        assertTrue(FakeIds.LONG_TITLE_MOVIE in folderItems())
        assertTrue(FakeIds.LONG_TITLE_MOVIE in flagged)

        repository.remove(FakeIds.LONG_TITLE_MOVIE)
        assertFalse(FakeIds.LONG_TITLE_MOVIE in folderItems())
        assertFalse(FakeIds.LONG_TITLE_MOVIE in flagged)
        assertEquals(2, flagToggles)
    }

    @Test
    fun `flag set on the site stays set when the title is added here`() = runTest {
        flagged += FakeIds.LONG_TITLE_MOVIE
        val repository = repository(backgroundScope)
        runCurrent()

        repository.add(title(FakeIds.LONG_TITLE_MOVIE))

        // Первый переключатель снял флаг — второй вернул: отметка на сайте осталась.
        assertTrue(FakeIds.LONG_TITLE_MOVIE in flagged)
        assertEquals(2, flagToggles)
    }

    @Test
    fun `adopting a title marked elsewhere fills the folder without touching the flag`() = runTest {
        val repository = repository(backgroundScope)
        runCurrent()

        repository.adopt(title(FakeIds.LONG_TITLE_MOVIE))
        repository.adopt(title(FakeIds.LONG_TITLE_MOVIE))

        assertTrue(FakeIds.LONG_TITLE_MOVIE in folderItems())
        assertEquals(0, flagToggles)
    }

    @Test
    fun `signing out forgets the list, signing in again reads it back`() = runTest {
        val repository = repository(backgroundScope)
        runCurrent()

        signedIn.value = false
        runCurrent()
        assertTrue(repository.items.first().isEmpty())
        assertNull(settings.getStringOrNull("watchlist_cache"))

        signedIn.value = true
        runCurrent()
        assertTrue(repository.contains(FakeIds.HERO_MOVIE).first())
    }

    @Test
    fun `old local list moves to the server once`() = runTest {
        val legacy = """
            [{"id":${FakeIds.LONG_TITLE_MOVIE},"title":"Т","posterSmall":"","year":2020,"durationMinutes":90}]
        """
        settings.putString("favorite_items", legacy)
        repository(backgroundScope)
        runCurrent()

        assertTrue(FakeIds.LONG_TITLE_MOVIE in folderItems())
        assertTrue(settings.getBoolean("watchlist_migrated", false))
    }

    private fun TestScope.repository(scope: CoroutineScope) = KinopubWatchlistRepository(
        folders = folders,
        api = api,
        settings = settings,
        signedIn = signedIn,
        scope = scope,
    )

    private suspend fun folderItems(): List<Int> =
        folders.getBookmarkItems(FakeIds.WATCHLIST_FOLDER, page = 1).getOrNull()?.items.orEmpty().map { it.id }

    private fun title(id: Int) =
        WatchlistItem(id = id, title = "Тайтл $id", posterSmall = "", year = 2020, durationMinutes = 90)
}

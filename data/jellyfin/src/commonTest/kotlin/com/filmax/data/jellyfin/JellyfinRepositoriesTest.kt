package com.filmax.data.jellyfin

import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.GenreFilter
import com.filmax.core.domain.catalog.ReferenceGenre
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.RatingSource
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.domain.error.AppError
import com.filmax.core.domain.error.toAppError
import com.filmax.core.domain.watchlist.model.toWatchlistItem
import com.filmax.data.jellyfin.JellyfinFixtures.EPISODES
import com.filmax.data.jellyfin.JellyfinFixtures.EPISODE_S2E1
import com.filmax.data.jellyfin.JellyfinFixtures.HISTORY
import com.filmax.data.jellyfin.JellyfinFixtures.MOVIE
import com.filmax.data.jellyfin.JellyfinFixtures.MOVIE_ID
import com.filmax.data.jellyfin.JellyfinFixtures.SERIES
import com.filmax.data.jellyfin.JellyfinFixtures.SERIES_ID
import io.ktor.http.HttpMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Каталог и просмотр домашнего сервера на подменённом движке: что уходит на сервер и что приходит экранам. */
class JellyfinRepositoriesTest {

    private val server = FakeJellyfinServer(
        mapOf(
            "/Items/$SERIES_ID" to SERIES,
            "/Shows/$SERIES_ID/Episodes" to EPISODES,
            "/Items/$MOVIE_ID" to MOVIE,
            "/Items" to HISTORY,
            "/UserItems/$SERIES_ID/UserData" to """{"IsFavorite":true,"Played":false}""",
            "/Sessions/Playing" to "",
            "/Sessions/Playing/Progress" to "",
        ),
    ).apply { signIn() }

    private val videos = JellyfinVideos(server.api, server.ids)
    private val catalog = JellyfinCatalogRepository(server.context, videos)
    private val watching = JellyfinWatchingRepository(server.context, videos)

    @Test
    fun `series details keep numbered seasons and drop specials`() = runTest {
        val item = catalog.getItemDetails(server.ids.intFor(SERIES_ID)).getOrNull()

        assertEquals("Тьма", item?.title)
        assertEquals(ItemType.SERIES, item?.type)
        assertEquals(listOf(1 to 1, 2 to 1), item?.tracklist?.map { it.seasonNumber to it.number })
        assertEquals(listOf(0, 1), item?.tracklist?.map { it.watchStatus })
        assertEquals(600, item?.tracklist?.first()?.watchedSeconds)
        assertEquals(8.7, item?.rating?.primary)
        assertEquals(8.7, item?.rating?.of(RatingSource.COMMUNITY))
        assertNull(item?.rating?.of(RatingSource.KINOPOISK))
        assertEquals("Луис Хофман", item?.cast)
        assertEquals("Баран бо Одар", item?.director)
        assertEquals("tt5753856", item?.imdbId)
        assertTrue(item?.inWatchlist == true)
        assertTrue(item?.finished == true)
        assertEquals("Драма", item?.genres?.single()?.title)
        val poster = item?.posters?.medium.orEmpty()
        assertTrue(poster.startsWith("${FakeJellyfinServer.ADDRESS}/Items/$SERIES_ID/Images/Primary"), poster)
    }

    @Test
    fun `movie is one track with audio and a quality label`() = runTest {
        val track = catalog.getItemDetails(server.ids.intFor(MOVIE_ID)).getOrNull()?.tracklist?.single()

        assertEquals(1, track?.number)
        assertEquals(0, track?.seasonNumber)
        assertEquals("1080p", track?.files?.single()?.quality)
        assertEquals(listOf(1 to "rus", 2 to "eng"), track?.audios?.map { it.index to it.lang })
        assertEquals("Оригинал", track?.audios?.first()?.voiceAuthor)
        assertTrue(server.requestsTo("/Shows/$MOVIE_ID/Episodes").isEmpty())
    }

    @Test
    fun `unknown id is not found rather than a crash`() = runTest {
        val result = catalog.getItemDetails(42)

        assertIs<RequestResult.Error>(result)
        assertEquals(AppError.NotFound, result.toAppError())
    }

    @Test
    fun `requests without a session read as an expired session`() = runTest {
        server.session.clear()

        val result = catalog.getItems(ItemType.MOVIE, CatalogSort.UPDATED, page = 1)

        assertIs<RequestResult.Error>(result)
        assertEquals(AppError.Auth, result.toAppError())
        assertTrue(server.requests.isEmpty())
    }

    @Test
    fun `catalog filters become server query parameters`() = runTest {
        catalog.getItems(
            type = ItemType.ANIME,
            genre = null,
            filters = CatalogFilters(
                yearFrom = 2015,
                minRatings = mapOf(RatingSource.COMMUNITY to 7),
                only4k = true,
                onlyFinished = true,
            ),
            sort = SortOption(CatalogSort.RATING, ascending = true),
            page = 3,
        )

        val query = server.requestsTo("/Items").single().parameters
        assertEquals("Movie,Series", query["includeItemTypes"])
        assertEquals("Anime|Аниме|аниме", query["genres"])
        assertEquals("CommunityRating", query["sortBy"])
        assertEquals("Ascending", query["sortOrder"])
        assertEquals("2015-01-01T00:00:00Z", query["minPremiereDate"])
        assertEquals("7.0", query["minCommunityRating"])
        assertEquals("true", query["is4K"])
        assertEquals("Ended", query["seriesStatus"])
        assertEquals("60", query["startIndex"])
        assertEquals("u1", query["userId"])
    }

    @Test
    fun `kinopub reference genres are found by name on the server`() = runTest {
        val comedies = GenreFilter.Reference(ReferenceGenre.COMEDY)
        catalog.getItems(ItemType.MOVIE, comedies, CatalogFilters(), SortOption(CatalogSort.VIEWS), page = 1)

        val query = server.requestsTo("/Items").single().parameters
        assertEquals("Comedy|Комедия|комедия", query["genres"])
        assertNull(query["genreIds"])
    }

    @Test
    fun `server genres from the dictionary are filtered by id`() = runTest {
        val drama = server.ids.intFor("9e000000000000000000000000000001")

        catalog.getItems(ItemType.SERIES, GenreFilter.Listed(drama), CatalogFilters(), SortOption(CatalogSort.UPDATED))

        val query = server.requestsTo("/Items").single().parameters
        assertEquals("9e000000000000000000000000000001", query["genreIds"])
        assertEquals("DateLastContentAdded", query["sortBy"])
    }

    @Test
    fun `episodes in history become their series with the episode progress`() = runTest {
        val history = watching.getHistory().getOrNull().orEmpty()

        val entry = history.single()
        assertEquals(server.ids.intFor(SERIES_ID), entry.itemId)
        assertEquals("Тьма", entry.title)
        assertEquals(2, entry.progress?.season)
        assertEquals(1, entry.progress?.videoId)
        assertEquals(1200, entry.progress?.timeSeconds)
        assertEquals(3600, entry.progress?.durationSeconds)
        assertEquals(1_714_588_212_000, entry.lastSeenAt)
        assertTrue(entry.episodeThumbnail.orEmpty().contains("/Items/$EPISODE_S2E1/Images/Primary"))
    }

    @Test
    fun `progress starts a playback once and then reports ticks`() = runTest {
        val seriesId = server.ids.intFor(SERIES_ID)
        catalog.getItemDetails(seriesId)

        watching.saveProgressSerial(seriesId, season = 2, videoId = 1, timeSeconds = 30)
        watching.saveProgressSerial(seriesId, season = 2, videoId = 1, timeSeconds = 35)

        assertTrue(server.requestsTo("/Sessions/Playing").single().body.contains(EPISODE_S2E1))
        val progress = server.requestsTo("/Sessions/Playing/Progress").single().body
        assertTrue(progress.contains("\"PositionTicks\":350000000"), progress)
    }

    @Test
    fun `watchlist is the server favorites, and a title marked elsewhere is adopted without a request`() = runTest {
        // Область отменена: перечитывание списка по входу шло бы параллельно и путало подсчёт запросов.
        val watchlist = JellyfinWatchlistRepository(server.context, CoroutineScope(Job().apply { cancel() }))
        val series = assertNotNull(catalog.getItemDetails(server.ids.intFor(SERIES_ID)).getOrNull())
        val before = server.requests.size

        watchlist.adopt(series.toWatchlistItem())
        assertEquals(before, server.requests.size)
        watchlist.remove(series.id)
        watchlist.add(series.toWatchlistItem())

        val marks = server.requestsTo("/UserFavoriteItems/$SERIES_ID").map { it.method }
        assertEquals(listOf(HttpMethod.Delete, HttpMethod.Post), marks)
        assertEquals(listOf(series.id), watchlist.items.first().map { it.id })
    }
}

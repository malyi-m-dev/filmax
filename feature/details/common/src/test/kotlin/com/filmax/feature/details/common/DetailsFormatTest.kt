package com.filmax.feature.details.common

import com.filmax.core.domain.catalog.model.Duration
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemRating
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.catalog.model.Posters
import com.filmax.core.domain.person.CastMember
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val NOT_STARTED = -1

class DetailsFormatTest {

    @Test
    fun `seasons and episodes come sorted whatever the playlist order`() {
        val series = calculateSeriesData(
            listOf(episode(season = 2, number = 1), episode(season = 1, number = 2), episode(season = 1, number = 1)),
        )

        assertEquals(listOf(1, 2), series.seasons.map { (season, _) -> season })
        assertEquals(listOf(1, 2), series.seasons.first().second.map { it.number })
    }

    @Test
    fun `resume is the episode in progress`() {
        val inProgress = episode(season = 2, number = 3, status = WATCH_STATUS_IN_PROGRESS)
        val series = calculateSeriesData(
            listOf(
                episode(season = 1, number = 1, status = WATCH_STATUS_FINISHED),
                inProgress,
                episode(season = 2, number = 4),
            ),
        )

        assertEquals(inProgress, series.resume)
        assertEquals(1, series.resumeSeasonIndex)
    }

    @Test
    fun `resume moves on to the episode after the last watched one, across seasons`() {
        val nextSeasonOpener = episode(season = 2, number = 1)
        val series = calculateSeriesData(
            listOf(
                episode(season = 1, number = 1, status = WATCH_STATUS_FINISHED),
                episode(season = 1, number = 2, status = WATCH_STATUS_FINISHED),
                nextSeasonOpener,
            ),
        )

        assertEquals(nextSeasonOpener, series.resume)
        assertEquals(1, series.resumeSeasonIndex)
    }

    @Test
    fun `resume rewatches the last episode when everything is watched`() {
        val pilot = episode(season = 1, number = 1, status = WATCH_STATUS_FINISHED)
        val finale = episode(season = 1, number = 2, status = WATCH_STATUS_FINISHED)
        val series = calculateSeriesData(listOf(pilot, finale))

        assertEquals(finale, series.resume)
    }

    @Test
    fun `episodes panel points at the neighbour seasons and stops at the ends`() {
        val series = calculateSeriesData(
            listOf(episode(season = 3, number = 1), episode(season = 1, number = 1), episode(season = 2, number = 1)),
        )

        val middle = episodesPanel(series, selected = 2)
        assertEquals(3, middle.next?.season)
        assertEquals(1, middle.previous?.season)
        assertNull(episodesPanel(series, selected = 3).next)
        assertNull(episodesPanel(series, selected = 1).previous)
    }

    @Test
    fun `single season has no neighbours`() {
        val single = calculateSeriesData(listOf(episode(season = 1, number = 1), episode(season = 1, number = 2)))

        assertNull(episodesPanel(single, selected = 1).next)
        assertNull(episodesPanel(single, selected = 1).previous)
    }

    @Test
    fun `season starts on the episode to continue, else on the first unwatched, else on the first`() {
        val resume = episode(season = 2, number = 2, status = WATCH_STATUS_IN_PROGRESS)
        val series = calculateSeriesData(
            listOf(
                episode(season = 1, number = 1, status = WATCH_STATUS_FINISHED),
                episode(season = 1, number = 2),
                episode(season = 2, number = 1, status = WATCH_STATUS_FINISHED),
                resume,
                episode(season = 3, number = 1, status = WATCH_STATUS_FINISHED),
                episode(season = 3, number = 2, status = WATCH_STATUS_FINISHED),
            ),
        )

        assertEquals(resume.id, episodesPanel(series, selected = 1).next?.episodeId)
        assertEquals(episode(season = 1, number = 2).id, episodesPanel(series, selected = 2).previous?.episodeId)
        assertEquals(episode(season = 3, number = 1).id, episodesPanel(series, selected = 2).next?.episodeId)
        assertNull(episodesPanel(series, selected = 3).next)
    }

    @Test
    fun `nothing to resume in an untouched series`() {
        val series = calculateSeriesData(listOf(episode(season = 1, number = 1), episode(season = 1, number = 2)))

        assertNull(series.resume)
        assertEquals(0, series.resumeSeasonIndex)
    }

    @Test
    fun `volume speaks in seasons, or in episodes for a single season`() {
        val twoSeasons = listOf(episode(season = 1, number = 1), episode(season = 2, number = 1))
        val fiveEpisodes = (1..5).map { number -> episode(season = 1, number = number) }

        assertEquals("2 сезона", volumeOf(twoSeasons))
        assertEquals("5 серий", volumeOf(fiveEpisodes))
        assertEquals("1 серия", volumeOf(listOf(episode(season = 1, number = 1))))
    }

    @Test
    fun `season and episode words agree with the number`() {
        assertEquals("21 сезон", volumeOf(seasons(21)))
        assertEquals("22 сезона", volumeOf(seasons(22)))
        assertEquals("11 сезонов", volumeOf(seasons(11)))
        assertEquals("12 серий", volumeOf((1..12).map { number -> episode(season = 1, number = number) }))
    }

    @Test
    fun `movie volume is its duration with hours dropped when there are none`() {
        assertEquals("2 ч 46 мин", volumeLabel(movie(minutes = 166.0), series = null))
        assertEquals("46 мин", volumeLabel(movie(minutes = 46.0), series = null))
        assertNull(volumeLabel(movie(minutes = null), series = null))
    }

    @Test
    fun `cast with photos wins over the names string`() {
        val withPhoto = CastMember(name = "Актриса", character = "Героиня", photoUrl = "photo")

        assertEquals(listOf(withPhoto), resolveCast(listOf(withPhoto), rawCast = "Кто-то ещё"))
    }

    @Test
    fun `cast falls back to the comma separated names`() {
        val people = resolveCast(emptyList(), rawCast = " Первый Актёр,  , Вторая Актриса ")

        assertEquals(listOf("Первый Актёр", "Вторая Актриса"), people.map { it.name })
        assertTrue(people.all { it.photoUrl == null })
    }

    @Test
    fun `initials take two words`() {
        assertEquals("КР", initials("Киану  Ривз"))
        assertEquals("Ш", initials("Шер"))
    }

    @Test
    fun `series is decided by type, not by the number of tracks`() {
        assertTrue(isSeries(movie(minutes = 90.0).copy(type = ItemType.ANIME)))
        assertFalse(isSeries(movie(minutes = 90.0).copy(tracklist = listOf(episode(0, 1), episode(0, 2)))))
    }

    @Test
    fun `tv show goes by seasons like a series, a concert plays like a film`() {
        assertTrue(isSeries(movie(minutes = 50.0).copy(type = ItemType.TV_SHOW)))
        assertFalse(isSeries(movie(minutes = 120.0).copy(type = ItemType.CONCERT)))
        assertFalse(isSeries(movie(minutes = 90.0).copy(type = ItemType.DOCUMENTARY_MOVIE)))
    }

    private fun volumeOf(tracks: List<MediaTrack>): String? {
        val item = movie(minutes = null).copy(type = ItemType.SERIES, tracklist = tracks)
        return volumeLabel(item, calculateSeriesData(tracks))
    }

    private fun seasons(count: Int) = (1..count).map { season -> episode(season = season, number = 1) }
}

private fun episode(season: Int, number: Int, status: Int = NOT_STARTED) = MediaTrack(
    id = season * 1000 + number,
    number = number,
    seasonNumber = season,
    title = "",
    thumbnail = "",
    durationSeconds = 2700,
    files = emptyList(),
    audios = emptyList(),
    subtitles = emptyList(),
    watchStatus = status,
)

private fun movie(minutes: Double?) = Item(
    id = 1,
    title = "Фильм",
    type = ItemType.MOVIE,
    year = 2024,
    plot = "",
    director = "",
    cast = "",
    country = "",
    genres = emptyList(),
    rating = ItemRating(filmax = 0, filmaxPercentage = "", imdb = null, kinopoisk = null),
    posters = Posters(small = "", medium = "", big = "", wide = null),
    duration = Duration(averageMinutes = minutes, totalMinutes = null),
    tracklist = emptyList(),
    trailer = null,
    inWatchlist = false,
    finished = false,
)

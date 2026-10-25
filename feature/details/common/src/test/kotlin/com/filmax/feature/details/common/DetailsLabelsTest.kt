package com.filmax.feature.details.common

import com.filmax.core.domain.catalog.model.Duration
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemRating
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.catalog.model.Posters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Что играет главная кнопка деталей, что она о себе пишет и как подписаны шапка и серии. */
class DetailsLabelsTest {

    @Test
    fun `movie plays whole and its button shows length`() {
        val movie = item(ItemType.MOVIE, tracks = emptyList(), minutes = 131.0)

        val play = playButton(movie, series = null, seasonEpisodes = emptyList())

        assertEquals(NO_SEASON to MOVIE_VIDEO_ID, play?.let { it.season to it.videoId })
        assertEquals("Смотреть", play?.title)
        assertEquals("2 ч 11 мин", play?.subtitle)
        assertEquals("Смотреть", play?.compact)
        assertNull(play?.progress)
    }

    @Test
    fun `series continues the episode in progress with what is left of it`() {
        val tracks = listOf(episode(4, 6, status = FINISHED), episode(4, 7, status = IN_PROGRESS, title = "Мост"))
        val series = calculateSeriesData(tracks)
        val show = item(ItemType.SERIES, tracks)

        val play = checkNotNull(playButton(show, series, series.seasons.first().second))

        assertEquals(4 to 7, play.season to play.videoId)
        assertEquals("Продолжить", play.title)
        assertEquals("S4E7 «Мост» · осталось 30 мин", play.subtitle)
        assertEquals("Продолжить · S4E7", play.compact)
        assertEquals(LEFT_FRACTION, play.progress ?: 0f, TOLERANCE)
    }

    @Test
    fun `untouched series starts the first episode of the shown season`() {
        val tracks = listOf(episode(1, 1), episode(1, 2), episode(2, 1))
        val series = calculateSeriesData(tracks)
        val show = item(ItemType.SERIES, tracks)

        val play = checkNotNull(playButton(show, series, series.seasons.last().second))

        assertEquals(2 to 1, play.season to play.videoId)
        assertEquals("Смотреть", play.compact)
    }

    @Test
    fun `series without episodes has nothing to play`() {
        val show = item(ItemType.SERIES, tracks = emptyList())

        assertNull(playButton(show, calculateSeriesData(emptyList()), emptyList()))
    }

    @Test
    fun `heading names type, genre, year and country the way each platform shows them`() {
        val movie = item(ItemType.MOVIE, tracks = emptyList(), minutes = 131.0, country = "Норвегия, Дания")

        val heading = detailsHeading(movie, series = null)

        assertEquals("Фильм · Драма", heading.typeAndGenre)
        assertEquals("Фильм · 2024 · Норвегия", heading.overline)
        assertEquals("2024 · Норвегия, Дания", heading.originLine)
        assertEquals("Драма · 2 ч 11 мин", heading.metaLine)
        assertEquals("2024 · Норвегия, Дания · 2 ч 11 мин", heading.sheetMeta)
        assertEquals(listOf("7.9" to true, "7.5" to false), heading.ratingBadges.map { it.value to it.highlighted })
    }

    @Test
    fun `series sheet meta names the full volume`() {
        val tracks = (1..2).flatMap { season -> (1..3).map { number -> episode(season, number) } }
        val series = calculateSeriesData(tracks)
        val show = item(ItemType.SERIES, tracks)

        assertTrue(detailsHeading(show, series).sheetMeta.orEmpty().endsWith("2 сезона, 6 серий"))
        assertEquals("Сериал · 2 сезона", detailsHeading(show, series).overline)

        val single = calculateSeriesData(tracks.take(1))
        assertTrue(detailsHeading(show, single).sheetMeta.orEmpty().endsWith(" · 1 серия"))
    }

    @Test
    fun `episode row tells watched, resume and untouched apart`() {
        val watched = episodeRow(episode(4, 6, status = FINISHED, title = "Пепел"), resumeId = null)
        assertEquals("6. Пепел", watched.title)
        assertEquals("Просмотрено · 50 мин", watched.meta)
        assertTrue(watched.watched)

        val resume = episode(4, 7, status = IN_PROGRESS, title = "Тайный совет")
        val resumeRow = episodeRow(resume, resumeId = resume.id)
        assertEquals("Продолжить · осталось 30 мин", resumeRow.meta)
        assertTrue(resumeRow.resume)

        val untouched = episodeRow(episode(4, 8, title = ""), resumeId = resume.id)
        assertEquals("Серия 8", untouched.title)
        assertEquals("50 мин", untouched.meta)
        assertFalse(untouched.resume)
        assertNull(untouched.progress)
    }

    @Test
    fun `season chips spell out the selected season and number the rest`() {
        val series = calculateSeriesData(listOf(episode(4, 1), episode(5, 1)))

        val chips = episodesPanel(series, selected = 4).seasons

        assertEquals(listOf("Сезон 4", "Сезон 5"), chips.map { it.label })
        assertEquals(listOf("4", "5"), chips.map { it.compactLabel })
        assertEquals(listOf(true, false), chips.map { it.selected })
        val noSeasons = calculateSeriesData(listOf(episode(0, 3)))
        assertEquals("Серии", episodesPanel(noSeasons, selected = 0).seasons.single().label)
    }

    @Test
    fun `season summary counts episodes and averages duration`() {
        assertEquals("1 серия · ~50 мин", seasonSummary(listOf(episode(1, 1))))
        assertEquals("3 серии · ~50 мин", seasonSummary((1..3).map { episode(1, it) }))
        assertEquals("12 серий · ~50 мин", seasonSummary((1..12).map { episode(1, it) }))
        assertNull(seasonSummary(emptyList()))
    }

    @Test
    fun `cast line names three people at most`() {
        val people = resolveCast(emptyList(), rawCast = "Анна Лунд, Эрик Сунд, Мия Хольм, Четвёртый")

        assertEquals("В ролях: Анна Лунд, Эрик Сунд, Мия Хольм", castLine(people))
        assertNull(castLine(emptyList()))
    }

    private fun item(type: ItemType, tracks: List<MediaTrack>, minutes: Double? = null, country: String = "") = Item(
        id = 1,
        title = "Тайтл",
        type = type,
        year = 2024,
        plot = "",
        director = "",
        cast = "",
        country = country,
        genres = listOf(Genre(1, "Драма")),
        rating = ItemRating(filmax = 0, filmaxPercentage = "", imdb = "7.5", kinopoisk = "7.9"),
        posters = Posters(small = "", medium = "", big = "", wide = null),
        duration = Duration(averageMinutes = minutes, totalMinutes = null),
        tracklist = tracks,
        trailer = null,
        inWatchlist = false,
        finished = false,
    )

    private fun episode(season: Int, number: Int, status: Int = NOT_STARTED, title: String = "") = MediaTrack(
        id = season * EPISODE_ID_STEP + number,
        number = number,
        seasonNumber = season,
        title = title,
        thumbnail = "",
        durationSeconds = EPISODE_SECONDS,
        files = emptyList(),
        audios = emptyList(),
        subtitles = emptyList(),
        watchedSeconds = if (status == IN_PROGRESS) WATCHED_SECONDS else 0,
        watchStatus = status,
    )

    private companion object {
        const val NOT_STARTED = -1
        const val IN_PROGRESS = 0
        const val FINISHED = 1
        const val EPISODE_ID_STEP = 1000
        const val EPISODE_SECONDS = 3000
        const val WATCHED_SECONDS = 1200

        /** 1200 из 3000 секунд досмотрено — полоска на 40%, осталось ровно 30 минут. */
        const val LEFT_FRACTION = 0.4f
        const val TOLERANCE = 0.001f
    }
}

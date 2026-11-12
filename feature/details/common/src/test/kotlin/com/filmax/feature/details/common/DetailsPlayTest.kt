package com.filmax.feature.details.common

import com.filmax.core.domain.catalog.model.Duration
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemRating
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.catalog.model.Posters
import com.filmax.core.domain.catalog.model.VideoRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Что играет главная кнопка деталей и как она подписана на телефоне и на ТВ. */
class DetailsPlayTest {

    @Test
    fun `movie plays whole and its button shows length`() {
        val movie = item(ItemType.MOVIE, tracks = emptyList(), minutes = 131.0)

        val play = checkNotNull(DetailsState(item = movie).play)

        assertNull(play.video)
        assertEquals("Смотреть", play.title)
        assertEquals("Смотреть", play.titleWithEpisode)
        assertEquals("2 ч 11 мин", play.subtitle)
        assertNull(play.progress)
    }

    @Test
    fun `series continues the episode in progress with what is left of it`() {
        val tracks = listOf(episode(4, 6, status = FINISHED), episode(4, 7, status = IN_PROGRESS, title = "Мост"))
        val state = seriesState(tracks)

        val play = checkNotNull(state.play)

        assertEquals(VideoRef.Episode(season = 4, number = 7), play.video)
        assertEquals("Продолжить", play.title)
        assertEquals("Продолжить · S4E7", play.titleWithEpisode)
        assertEquals("S4E7 «Мост» · осталось 30 мин", play.subtitle)
        assertEquals(LEFT_FRACTION, play.progress ?: 0f, TOLERANCE)
    }

    @Test
    fun `untouched series starts the selected season, without a progress bar`() {
        val tracks = listOf(episode(1, 1, status = NOT_STARTED), episode(2, 1, status = NOT_STARTED))
        val state = seriesState(tracks).copy(selectedSeason = 2)

        val play = checkNotNull(state.play)

        assertEquals(VideoRef.Episode(season = 2, number = 1), play.video)
        assertEquals("Смотреть", play.titleWithEpisode)
        assertNull(play.progress)
    }

    @Test
    fun `episode outside a season is named by its number`() {
        val state = seriesState(listOf(episode(0, 3, status = IN_PROGRESS)))

        assertEquals("Продолжить · Серия 3", state.play?.titleWithEpisode)
    }

    @Test
    fun `series without episodes has nothing to play`() {
        assertNull(seriesState(emptyList()).play)
        assertNull(DetailsState().play)
    }

    @Test
    fun `overlines name the type with the genre on the phone and with the origin on tv`() {
        val state = DetailsState(item = item(ItemType.MOVIE, emptyList()).copy(country = "Норвегия, Дания"))

        assertEquals("Фильм · Драма", state.genreOverline)
        assertEquals("Фильм · 2024 · Норвегия", state.originOverline)
    }

    @Test
    fun `selected season chip is spelled out, the rest are numbers`() {
        assertEquals("Сезон 4", seasonChipLabel(4, selected = true))
        assertEquals("5", seasonChipLabel(5, selected = false))
        assertEquals("Серии", seasonChipLabel(0, selected = true))
    }

    @Test
    fun `season summary counts episodes and averages duration`() {
        assertEquals("1 серия · ~50 мин", seasonState(1).seasonSummary)
        assertEquals("3 серии · ~50 мин", seasonState(3).seasonSummary)
        assertEquals("12 серий · ~50 мин", seasonState(12).seasonSummary)
        assertNull(seriesState(emptyList()).seasonSummary)
    }

    @Test
    fun `description meta names all countries and the full volume`() {
        val movie = item(ItemType.MOVIE, emptyList(), minutes = 131.0).copy(country = "Норвегия, Дания")
        assertEquals("2024 · Норвегия, Дания · 2 ч 11 мин", DetailsState(item = movie).descriptionMeta)

        val tracks = (1..2).flatMap { season -> (1..3).map { number -> episode(season, number, NOT_STARTED) } }
        val twoSeasons = seriesState(tracks, country = "США")
        assertTrue(twoSeasons.descriptionMeta.orEmpty().endsWith("2 сезона, 6 серий"), twoSeasons.descriptionMeta)

        val single = seriesState(tracks.take(1), country = "США")
        assertTrue(single.descriptionMeta.orEmpty().endsWith(" · 1 серия"), single.descriptionMeta)
    }

    private fun seasonState(episodes: Int) = seriesState((1..episodes).map { episode(1, it, NOT_STARTED) })

    private fun seriesState(tracks: List<MediaTrack>, country: String = ""): DetailsState {
        val series = calculateSeriesData(tracks)
        return DetailsState(
            item = item(ItemType.SERIES, tracks).copy(country = country),
            series = series,
            selectedSeason = series.seasons.getOrNull(series.resumeSeasonIndex)?.first,
        )
    }

    private fun item(type: ItemType, tracks: List<MediaTrack>, minutes: Double? = null) = Item(
        id = 1,
        title = "Тайтл",
        type = type,
        year = 2024,
        plot = "",
        director = "",
        cast = "",
        country = "",
        genres = listOf(Genre(1, "Драма")),
        rating = ItemRating(),
        posters = Posters(small = "", medium = "", big = "", wide = null),
        duration = Duration(averageMinutes = minutes, totalMinutes = null),
        tracklist = tracks,
        trailer = null,
        inWatchlist = false,
        finished = false,
    )

    private fun episode(season: Int, number: Int, status: Int, title: String = "") = MediaTrack(
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
        const val FINISHED = 1
        const val IN_PROGRESS = 0
        const val EPISODE_ID_STEP = 1000
        const val EPISODE_SECONDS = 3000
        const val WATCHED_SECONDS = 1200

        /** 1200 из 3000 секунд досмотрено — полоска на 40%, осталось ровно 30 минут. */
        const val LEFT_FRACTION = 0.4f
        const val TOLERANCE = 0.001f
    }
}

package com.filmax.feature.details.mobile

import com.filmax.core.domain.catalog.model.Duration
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemRating
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.catalog.model.Posters
import com.filmax.feature.details.common.calculateSeriesData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Что играет главная кнопка деталей и что она о себе пишет. */
class DetailsLabelsTest {

    @Test
    fun `movie plays whole and its button shows length`() {
        val movie = item(ItemType.MOVIE, tracks = emptyList(), minutes = 131.0)

        val target = playTarget(movie, series = null, seasonEpisodes = emptyList())

        assertEquals(PlayTarget(NO_SEASON, MOVIE_VIDEO_ID, episode = null), target)
        assertEquals(PlayLabel("Смотреть", "2 ч 11 мин", progress = null), playLabel(movie, null, target))
    }

    @Test
    fun `series continues the episode in progress with what is left of it`() {
        val tracks = listOf(episode(4, 6, status = FINISHED), episode(4, 7, status = IN_PROGRESS, title = "Мост"))
        val series = calculateSeriesData(tracks)
        val show = item(ItemType.SERIES, tracks)

        val target = playTarget(show, series, series.seasons.first().second)
        val label = playLabel(show, series, target)

        assertEquals(4 to 7, target?.let { it.season to it.videoId })
        assertEquals("Продолжить", label.title)
        assertEquals("S4E7 «Мост» · осталось 30 мин", label.subtitle)
        assertEquals(LEFT_FRACTION, label.progress ?: 0f, TOLERANCE)
    }

    @Test
    fun `series without episodes has nothing to play`() {
        val show = item(ItemType.SERIES, tracks = emptyList())

        assertNull(playTarget(show, calculateSeriesData(emptyList()), emptyList()))
    }

    @Test
    fun `overline names the type and the first genre`() {
        assertEquals("Сериал · Драма", detailsOverline(item(ItemType.SERIES, emptyList())))
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
        rating = ItemRating(filmax = 0, filmaxPercentage = "", imdb = null, kinopoisk = null),
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

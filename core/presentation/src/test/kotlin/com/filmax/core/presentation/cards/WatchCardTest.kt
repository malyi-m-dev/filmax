package com.filmax.core.presentation.cards

import com.filmax.core.domain.catalog.model.VideoRef
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.domain.watching.model.WatchProgress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Подписи карточек «Моего»: что смотрели, когда, сколько и куда вести плеер. */
class WatchCardTest {

    @Test
    fun `episode is named by season and number, a movie is just a movie`() {
        val episode = watchCard(entry(progress(season = 4, videoId = 7)))
        val movie = watchCard(entry(progress(season = 0, videoId = 1)))

        assertEquals("S4E7", episode.whatWatched)
        assertEquals("S4E7", episode.episode)
        assertEquals(VideoRef.Episode(season = 4, number = 7), episode.video)
        assertEquals("Фильм", movie.whatWatched)
        assertNull(movie.episode)
        assertEquals("Фильм", watchCard(entry(progress = null)).whatWatched)
    }

    @Test
    fun `phone tells when and how far, tv tells which episode and when`() {
        val started = watchCard(entry(progress(time = 1350)), time = "21:10")
        val finished = watchCard(entry(progress(time = 2990)))

        assertEquals("21:10 · на 45%", started.whenWatched)
        assertEquals("S1E1 · 21:10", started.episodeAndTime)
        assertEquals("осталось 27 мин", started.remaining)
        assertEquals("просмотрено", finished.whenWatched)
        assertEquals("просмотрено", finished.remaining)
        assertNull(watchCard(entry(progress = null)).whenWatched)
    }

    private fun entry(progress: WatchProgress?) =
        WatchHistory(itemId = 1, title = "Тайтл", posterSmall = null, progress = progress)

    private fun progress(season: Int = 1, videoId: Int = 1, time: Int = 0) = WatchProgress(
        status = 0,
        timeSeconds = time,
        durationSeconds = DURATION,
        videoId = videoId,
        season = season,
    )

    private companion object {
        const val DURATION = 3000
    }
}

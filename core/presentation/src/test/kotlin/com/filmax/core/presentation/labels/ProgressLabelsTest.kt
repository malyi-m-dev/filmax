package com.filmax.core.presentation.labels

import com.filmax.core.domain.watching.model.WatchProgress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Подписи прогресса: что смотрели, сколько осталось и сколько посмотрели. */
class ProgressLabelsTest {

    @Test
    fun `episode is named by season and number, a movie has no code`() {
        assertEquals("S4E7", episodeCode(progress(season = 4, videoId = 7)))
        assertNull(episodeCode(progress(season = 0, videoId = 1)))
        assertNull(episodeCode(null))
    }

    @Test
    fun `watched share tells how far, watched to the end says so`() {
        assertEquals("на 45%", watchedShare(progress(time = 1350)))
        assertEquals("просмотрено", watchedShare(progress(time = 2990)))
        assertNull(watchedShare(progress(time = 0)))
        assertNull(watchedShare(null))
    }

    @Test
    fun `remaining label counts minutes to the end`() {
        assertEquals("осталось 27 мин", remainingLabel(progress(time = 1350)))
        assertEquals("просмотрено", remainingLabel(progress(time = 2990)))
        assertNull(remainingLabel(null))
    }

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

package com.filmax.feature.library.mobile

import com.filmax.core.domain.watching.model.WatchProgress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Подписи строки истории: что смотрели и сколько. */
class MineLabelsTest {

    @Test
    fun `episode is named by season and number, a movie is just a movie`() {
        assertEquals("S4E7", historySubtitle(progress(season = 4, videoId = 7)))
        assertEquals("Фильм", historySubtitle(progress(season = 0, videoId = 1)))
        assertEquals("Фильм", historySubtitle(null))
    }

    @Test
    fun `meta tells when and how far, watched to the end says so`() {
        assertEquals("21:10 · на 45%", historyMeta(progress(time = 1350), "21:10"))
        assertEquals("просмотрено", historyMeta(progress(time = 2990), null))
        assertNull(historyMeta(null, null))
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

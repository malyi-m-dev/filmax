package com.filmax.feature.details.tv

import com.filmax.core.domain.catalog.model.MediaTrack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TvDetailsLabelsTest {

    @Test
    fun `episode row tells watched, resume and untouched apart`() {
        val watched = episodeRow(episode(4, 6, status = FINISHED, title = "Пепел"), resumeId = null)
        assertEquals("6. Пепел", watched.title)
        assertEquals("Просмотрено · 45 мин", watched.meta)
        assertTrue(watched.watched)

        val resume = episode(4, 7, status = IN_PROGRESS, title = "Тайный совет", watchedSeconds = 240)
        val resumeRow = episodeRow(resume, resumeId = resume.id)
        assertEquals("Продолжить · осталось 41 мин", resumeRow.meta)
        assertTrue(resumeRow.resume)

        val untouched = episodeRow(episode(4, 8, title = ""), resumeId = resume.id)
        assertEquals("Серия 8", untouched.title)
        assertEquals("45 мин", untouched.meta)
        assertFalse(untouched.resume)
        assertNull(untouched.progress)
    }

    private fun episode(
        season: Int,
        number: Int,
        status: Int = NOT_STARTED,
        title: String = "",
        watchedSeconds: Int = 0,
    ) = MediaTrack(
        id = season * 1000 + number,
        number = number,
        seasonNumber = season,
        title = title,
        thumbnail = "",
        durationSeconds = 2700,
        files = emptyList(),
        audios = emptyList(),
        subtitles = emptyList(),
        watchedSeconds = watchedSeconds,
        watchStatus = status,
    )

    private companion object {
        const val NOT_STARTED = -1
        const val IN_PROGRESS = 0
        const val FINISHED = 1
    }
}

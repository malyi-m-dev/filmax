package com.filmax.feature.details.tv

import com.filmax.core.domain.catalog.model.MediaTrack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TvDetailsLabelsTest {

    @Test
    fun `play label names the resume episode`() {
        assertEquals("Смотреть", playLabel(null))
        assertEquals("Продолжить · S4E7", playLabel(episode(season = 4, number = 7)))
        assertEquals("Продолжить · Серия 3", playLabel(episode(season = 0, number = 3)))
    }

    @Test
    fun `selected season chip is spelled out, the rest are numbers`() {
        assertEquals("Сезон 4", seasonChipLabel(4, selected = true))
        assertEquals("5", seasonChipLabel(5, selected = false))
        assertEquals("Серии", seasonChipLabel(0, selected = true))
    }

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

    @Test
    fun `season summary counts episodes and averages duration`() {
        assertEquals("1 серия · ~45 мин", seasonSummary(listOf(episode(1, 1))))
        assertEquals("3 серии · ~45 мин", seasonSummary((1..3).map { episode(1, it) }))
        assertEquals("12 серий · ~45 мин", seasonSummary((1..12).map { episode(1, it) }))
        assertNull(seasonSummary(emptyList()))
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

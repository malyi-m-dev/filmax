package com.filmax.core.presentation.labels

import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.watching.model.WatchProgress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TitleLabelsTest {

    @Test
    fun `rating keeps one decimal`() {
        assertEquals("8.3", ratingLabel("8.312"))
        assertEquals("7.0", ratingLabel(7.0))
    }

    @Test
    fun `zero or missing rating means no rating, not zero points`() {
        assertNull(ratingLabel("0"))
        assertNull(ratingLabel(0.0))
        assertNull(ratingLabel(null as String?))
        assertNull(ratingLabel("нет"))
    }

    @Test
    fun `poster meta skips empty parts`() {
        assertEquals("Фильм · 2024", posterMeta("Фильм", 2024))
        assertEquals("2020", posterMeta(null, 2020))
        assertNull(posterMeta("", 0))
    }

    @Test
    fun `type label speaks the viewer language, not the API one`() {
        assertEquals("Сериал", typeLabel(ItemType.SERIES))
        assertEquals("Документальный", typeLabel(ItemType.DOCUMENTARY))
        assertEquals("Документальный", typeLabel(ItemType.DOCUMENTARY_MOVIE))
        assertEquals("ТВ-шоу", typeLabel(ItemType.TV_SHOW))
        assertEquals("Концерт", typeLabel(ItemType.CONCERT))
    }

    @Test
    fun `duration drops hours or minutes when they are zero`() {
        assertEquals("46 мин", durationLabel(46))
        assertEquals("1 ч", durationLabel(60))
        assertEquals("1 ч 46 мин", durationLabel(106))
    }

    @Test
    fun `continue meta tells the season and what is left`() {
        val meta = continueMeta(progress(season = 2, timeSeconds = 600, durationSeconds = 1680))

        assertEquals("S2 · осталось 18 мин", meta)
    }

    @Test
    fun `continue meta says watched when nothing is left`() {
        assertEquals("просмотрено", continueMeta(progress(season = 0, timeSeconds = 1680, durationSeconds = 1680)))
    }

    @Test
    fun `continue meta is silent without progress`() {
        assertNull(continueMeta(null))
    }

    private fun progress(season: Int, timeSeconds: Int, durationSeconds: Int) = WatchProgress(
        status = 0,
        timeSeconds = timeSeconds,
        durationSeconds = durationSeconds,
        videoId = 1,
        season = season,
    )
}

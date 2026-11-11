package com.filmax.feature.player.common

import com.filmax.core.domain.catalog.model.SubtitleTrack
import com.filmax.core.domain.playback.PlaybackSettings
import kotlin.test.Test
import kotlin.test.assertEquals

/** Меню субтитров: «Выкл» первым, язык из настроек выбран, если он есть у трека. */
class SubtitleOptionsTest {

    private val options = subtitleOptions(
        listOf(
            SubtitleTrack(lang = "rus", url = "https://cdn/ru.srt", shiftMs = 0),
            SubtitleTrack(lang = "eng", url = "https://cdn/en.vtt", shiftMs = 0),
        ),
    )

    @Test
    fun `off comes first, then the languages of the track`() {
        assertEquals(listOf(PlaybackSettings.SubtitleOff, "Русский", "English"), options.map { it.label })
    }

    @Test
    fun `language from settings is picked when the track has it, otherwise subtitles stay off`() {
        assertEquals("rus", options.preferred("Русский").lang)
        assertEquals(null, options.preferred("Deutsch").lang)
        assertEquals(null, options.preferred(PlaybackSettings.SubtitleOff).lang)
    }
}

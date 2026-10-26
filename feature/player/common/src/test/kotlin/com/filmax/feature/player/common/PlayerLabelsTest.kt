package com.filmax.feature.player.common

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.common.getOrNull
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeCatalogRepository
import com.filmax.data.fake.FakeIds
import com.filmax.data.fake.FakeLatency
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Подписи плеера: строка под названием, значения вкладок, строки вариантов, серии и скорость. */
class PlayerLabelsTest {

    private val series: Item = runBlocking {
        val catalog = FakeCatalogRepository(FakeBackend(latency = FakeLatency.None))
        checkNotNull(catalog.getItemDetails(FakeIds.LONG_SERIES).getOrNull())
    }
    private val episode: MediaTrack = series.tracklist.first { it.seasonNumber == 4 && it.number == 7 }

    @Test
    fun `subtitle line names season, episode and title, and says paused when the panel is open`() {
        val state = PlayerState(item = series, track = episode)

        assertEquals("Сезон 4 · Серия 7 · ${episode.title}", playerSubtitle(state, paused = false))
        assertEquals("Сезон 4 · Серия 7 · на паузе", playerSubtitle(state, paused = true))
        assertEquals("${series.title} · S4E7", panelTitle(state))
    }

    @Test
    fun `tab values show the current choice shortly`() {
        val state = PlayerState(
            audioTracks = listOf(
                AudioOption("1. Русский", 0, languageCode = "rus"),
                AudioOption("2. English", 1, "eng"),
            ),
            currentAudio = "2. English",
            subtitles = listOf(SubtitleOption("Выкл", null), SubtitleOption("Русский", "rus")),
            currentSubtitle = "Выкл",
            qualities = listOf(StreamQuality("1080p", listOf("https://stream"))),
            currentQuality = "1080p",
            currentSpeed = 1.25f,
        )

        assertEquals("EN", tabValue(PlayerTab.Audio, state))
        assertEquals("Выкл", tabValue(PlayerTab.Subtitles, state))
        assertEquals("RU", tabValue(PlayerTab.Subtitles, state.copy(currentSubtitle = "Русский")))
        assertEquals("1080p", tabValue(PlayerTab.Quality, state))
        assertEquals("1.25×", tabValue(PlayerTab.Speed, state))
        assertNull(tabValue(PlayerTab.Episodes, state))
    }

    @Test
    fun `audio option splits language and voice from studio and channels`() {
        val option = AudioOption(
            "1. Русский · Многоголосый · LostFilm",
            0,
            "rus",
            "Многоголосый",
            "LostFilm",
            channels = 6,
        )

        assertEquals(OptionText("Русский · Многоголосый", "LostFilm", "5.1"), audioOptionText(option))
        assertEquals(OptionText("English", null, null), audioOptionText(AudioOption("2. English", 1, "eng")))
    }

    @Test
    fun `options mark the current choice and map to their events`() {
        val state = PlayerState(
            qualities = listOf(StreamQuality("1080p", listOf("a")), StreamQuality("720p", listOf("b"))),
            currentQuality = "720p",
        )

        assertEquals(listOf(false, true), optionItems(PlayerTab.Quality, state).map { it.selected })
        assertEquals(PlayerEvent.SelectQuality("1080p"), optionEvent(PlayerTab.Quality, "1080p"))
        assertEquals(PlayerEvent.SelectAudio("x"), optionEvent(PlayerTab.Audio, "x"))
        assertNull(optionEvent(PlayerTab.Speed, "x"))
    }

    @Test
    fun `episode labels tell title, length and progress apart`() {
        val watched = episode.copy(watchStatus = 1)
        val fresh = episode.copy(watchStatus = -1, watchedSeconds = 0, durationSeconds = 58 * 60)

        assertEquals("58 мин", episodeLength(fresh))
        assertNull(episodeLength(fresh.copy(durationSeconds = 0)))
        assertEquals(1f, episodeProgress(watched))
        assertNull(episodeProgress(fresh))
        assertEquals("7. Серия 7", episodeTitle(episode.copy(title = "")))
        assertEquals("S4E8", episodeCode(episode.copy(number = 8)))
        assertEquals("Серия 3", episodeCode(episode.copy(seasonNumber = 0, number = 3)))
    }

    @Test
    fun `speed tiles say 1x instead of a word and mark the landmarks`() {
        assertEquals("1×", speedValue(1f))
        assertEquals("1.5×", speedValue(1.5f))
        assertEquals("обычная", speedCaption(1f))
        assertEquals("быстро", speedCaption(2f))
        assertNull(speedCaption(1.25f))
    }

    @Test
    fun `audio tab names surround channels but not plain stereo`() {
        val surround = PlayerState(
            audioTracks = listOf(AudioOption("1. Русский", 0, "rus", channels = 6)),
            currentAudio = "1. Русский",
        )
        val stereo = surround.copy(audioTracks = listOf(AudioOption("1. Русский", 0, "rus", channels = 2)))

        assertEquals("RU · 5.1", tabValue(PlayerTab.Audio, surround))
        assertEquals("RU", tabValue(PlayerTab.Audio, stereo))
        // Плашка транспорта — только язык, как в макете: «Аудио · RU».
        assertEquals("Аудио · RU", statusLabel(PlayerTab.Audio, surround))
    }

    @Test
    fun `tv speeds are six tiles from half to double`() {
        assertEquals(listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f), PresetSpeeds)
    }
}

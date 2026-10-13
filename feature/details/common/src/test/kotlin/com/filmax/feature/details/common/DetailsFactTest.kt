package com.filmax.feature.details.common

import com.filmax.core.domain.catalog.model.AudioTrack
import com.filmax.core.domain.catalog.model.Duration
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemRating
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.catalog.model.Posters
import com.filmax.core.domain.catalog.model.SubtitleTrack
import com.filmax.core.domain.catalog.model.VideoFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DetailsFactTest {

    @Test
    fun `facts list country, genre, duration, languages and the best quality`() {
        val facts = detailsFacts(movie(), series = null).associate { it.label to it.value }

        assertEquals("Норвегия, Дания", facts["Страна"])
        assertEquals("Фэнтези, Приключения", facts["Жанр"])
        assertEquals("2 ч 11 мин", facts["Длительность"])
        assertEquals("Русский, English", facts["Аудио"])
        assertEquals("Русский", facts["Субтитры"])
        assertEquals("до 2160p", facts["Качество"])
    }

    @Test
    fun `empty facts are left out instead of showing blank cells`() {
        val bare = movie().copy(country = "", genres = emptyList(), tracklist = emptyList())

        val labels = detailsFacts(bare, series = null).map { it.label }

        assertEquals(listOf("Длительность"), labels)
    }

    @Test
    fun `quality ranks 4K above numeric resolutions`() {
        val track = movie().tracklist.first().copy(files = listOf(file("1080p"), file("4K"), file("720p")))

        val quality = detailsFacts(movie().copy(tracklist = listOf(track)), null).single { it.label == "Качество" }

        assertTrue(quality.value.contains("4K"))
    }

    private fun movie() = Item(
        id = 1,
        title = "Дом на краю карты",
        type = ItemType.MOVIE,
        year = 2022,
        plot = "",
        director = "",
        cast = "",
        country = "Норвегия, Дания",
        genres = listOf(Genre(id = 1, title = "Фэнтези"), Genre(id = 2, title = "Приключения")),
        rating = ItemRating(filmax = 0, filmaxPercentage = "", imdb = null, kinopoisk = null),
        posters = Posters(small = "", medium = "", big = "", wide = null),
        duration = Duration(averageMinutes = 131.0, totalMinutes = null),
        tracklist = listOf(
            MediaTrack(
                id = 1,
                number = 1,
                seasonNumber = 0,
                title = "",
                thumbnail = "",
                durationSeconds = 7860,
                files = listOf(file("1080p"), file("2160p")),
                audios = listOf(audio("rus"), audio("eng"), audio("rus")),
                subtitles = listOf(SubtitleTrack(lang = "rus", url = "", shiftMs = 0)),
            ),
        ),
        trailer = null,
        inWatchlist = false,
        finished = false,
    )

    private fun file(quality: String) = VideoFile(quality = quality, hls4 = null, hls = null, http = null)

    private fun audio(lang: String) = AudioTrack(id = 0, index = 0, lang = lang, title = null, channels = 2)
}

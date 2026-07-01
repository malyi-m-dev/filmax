package com.filmax.feature.details.common

import com.filmax.core.domain.catalog.model.AudioTrack
import com.filmax.core.domain.catalog.model.Duration
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemRating
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.catalog.model.Posters
import com.filmax.core.domain.catalog.model.VideoFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DetailsUiMapperTest {

    @Test
    fun `metaLine contains duration country and top genres`() {
        val ui = movie(
            year = 2016,
            averageMinutes = 135.0,
            country = "США",
            genres = listOf("Драма", "Триллер", "Комедия"),
        ).toDetailsUi(similar = emptyList())

        assertEquals("2016  ·  2ч 15м  ·  США  ·  Драма · Триллер", ui.metaLine)
        assertEquals("2016  ·  США  ·  Драма · Триллер", ui.metaLineNoDuration)
    }

    @Test
    fun `metaLine skips missing parts`() {
        val ui = movie(year = 2020, averageMinutes = null, country = "", genres = emptyList())
            .toDetailsUi(similar = emptyList())

        assertEquals("2020", ui.metaLine)
    }

    @Test
    fun `cast is parsed into members with initials`() {
        val ui = movie(cast = "Райан Гослинг, Эмма Стоун,  ,Джон К").toDetailsUi(similar = emptyList())

        assertEquals(
            listOf(
                CastMemberUi("Райан Гослинг", "РГ"),
                CastMemberUi("Эмма Стоун", "ЭС"),
                CastMemberUi("Джон К", "ДК"),
            ),
            ui.castMembers,
        )
    }

    @Test
    fun `episodes are grouped by season and sorted by number`() {
        val ui = series(
            episode(id = 1, season = 2, number = 2),
            episode(id = 2, season = 1, number = 1),
            episode(id = 3, season = 2, number = 1),
        ).toDetailsUi(similar = emptyList())

        assertEquals(listOf(1, 2), ui.seasons.map { it.number })
        assertEquals(listOf("Сезон 1", "Сезон 2"), ui.seasons.map { it.chipLabel })
        assertEquals(listOf(3, 1), ui.seasons[1].episodes.map { it.id })
        assertEquals("1 серия", ui.seasons[0].countLabel)
        assertEquals("2 серии", ui.seasons[1].countLabel)
    }

    @Test
    fun `resume prefers in-progress episode over finished`() {
        val ui = series(
            episode(id = 1, season = 1, number = 1, watchStatus = 1),
            episode(id = 2, season = 2, number = 1, watchStatus = 0, watchedSeconds = 600, durationSeconds = 1200),
            episode(id = 3, season = 2, number = 2),
        ).toDetailsUi(similar = emptyList())

        assertEquals(2, ui.resume?.episodeId)
        assertEquals("Сезон 2 · Серия 1", ui.resume?.positionLabel)
        assertEquals(0.5f, ui.resume?.progress)
        assertEquals(1, ui.resumeSeasonIndex)
    }

    @Test
    fun `resume falls back to last finished episode without progress bar when duration unknown`() {
        val ui = series(
            episode(id = 1, season = 1, number = 1, watchStatus = 1, durationSeconds = 0),
            episode(id = 2, season = 1, number = 2),
        ).toDetailsUi(similar = emptyList())

        assertEquals(1, ui.resume?.episodeId)
        assertNull(ui.resume?.progress)
    }

    @Test
    fun `episode meta line marks resume episode`() {
        val ui = series(
            episode(id = 1, season = 1, number = 1, watchStatus = 0, durationSeconds = 2520),
            episode(id = 2, season = 1, number = 2, durationSeconds = 0),
        ).toDetailsUi(similar = emptyList())

        val episodes = ui.seasons.single().episodes
        assertEquals("42 мин · продолжить", episodes[0].metaLine)
        assertEquals("", episodes[1].metaLine)
        assertEquals("Серия 1. Пилот", episodes[0].label)
    }

    @Test
    fun `single track movie is not series like`() {
        val ui = movie().toDetailsUi(similar = emptyList())
        assertFalse(ui.isSeriesLike)

        val seriesUi = series(episode(id = 1, season = 1, number = 1)).toDetailsUi(similar = emptyList())
        assertTrue(seriesUi.isSeriesLike)
    }

    @Test
    fun `quality badges combine video qualities and audio languages`() {
        val track = episode(id = 1, season = 1, number = 1).copy(
            files = listOf(VideoFile("1080p", null, null, null), VideoFile("720p", null, null, null)),
            audios = listOf(
                AudioTrack(id = 1, lang = "rus", title = null, channels = 2),
                AudioTrack(id = 2, lang = "eng", title = null, channels = 2),
            ),
        )
        val ui = series(track).toDetailsUi(similar = emptyList())

        assertEquals(listOf("1080p", "720p"), ui.videoQualities)
        assertEquals(listOf("1080p", "720p", "Русский", "Eng"), ui.qualityBadges)
    }

    @Test
    fun `stats grid uses dashes and placeholders for missing data`() {
        val ui = movie(director = "", genres = emptyList(), averageMinutes = null)
            .toDetailsUi(similar = emptyList())

        assertEquals(DetailsStatUi("Длительность", "?", "мин"), ui.stats[1])
        assertEquals(DetailsStatUi("Режиссёр", "—", ""), ui.stats[2])
        assertEquals(DetailsStatUi("Жанр", "—", "—"), ui.stats[3])
    }

    @Test
    fun `seasons caption uses russian plural rules`() {
        fun captionFor(seasonCount: Int): String {
            val episodes = (1..seasonCount).map { season ->
                episode(id = season, season = season, number = 1)
            }
            return series(*episodes.toTypedArray()).toDetailsUi(similar = emptyList()).seasonsCaption
        }

        assertEquals("Сериал · 1 сезон", captionFor(1))
        assertEquals("Сериал · 3 сезона", captionFor(3))
        assertEquals("Сериал · 5 сезонов", captionFor(5))
        assertEquals("Сериал · 11 сезонов", captionFor(11))
    }

    @Test
    fun `play episode id in state prefers resume then first of selected season`() {
        val details = series(
            episode(id = 10, season = 1, number = 1),
            episode(id = 20, season = 2, number = 1),
        ).toDetailsUi(similar = emptyList())
        val state = DetailsUiState(loading = false, details = details, selectedSeasonIndex = 1)

        assertEquals(20, state.playEpisodeId)
        assertEquals("Сезон 2", state.currentSeason?.chipLabel)
    }

    // ─── Фикстуры ───────────────────────────────────────────────────────────

    private fun movie(
        year: Int = 2016,
        averageMinutes: Double? = 135.0,
        country: String = "США",
        genres: List<String> = listOf("Драма"),
        cast: String = "Райан Гослинг, Эмма Стоун",
        director: String = "Дэмьен Шазелл",
        tracklist: List<MediaTrack> = emptyList(),
        type: ItemType = ItemType.MOVIE,
    ): Item = Item(
        id = 1,
        title = "Ла-Ла Ленд",
        type = type,
        year = year,
        plot = "Джаз и мечты.",
        director = director,
        cast = cast,
        country = country,
        genres = genres.mapIndexed { index, title -> Genre(id = index, title = title) },
        rating = ItemRating(filmax = 90, filmaxPercentage = "90", imdb = "8.0", kinopoisk = "8.2"),
        posters = Posters(small = "s", medium = "m", big = "b", wide = "w"),
        duration = Duration(averageMinutes = averageMinutes, totalMinutes = averageMinutes?.toInt()),
        tracklist = tracklist,
        trailer = null,
        inWatchlist = false,
        finished = false,
    )

    private fun series(vararg episodes: MediaTrack): Item =
        movie(tracklist = episodes.toList(), type = ItemType.SERIES)

    private fun episode(
        id: Int,
        season: Int,
        number: Int,
        watchStatus: Int = -1,
        watchedSeconds: Int = 0,
        durationSeconds: Int = 2520,
    ): MediaTrack = MediaTrack(
        id = id,
        number = number,
        seasonNumber = season,
        title = if (number == 1) "Пилот" else "",
        thumbnail = "",
        durationSeconds = durationSeconds,
        files = emptyList(),
        audios = emptyList(),
        subtitles = emptyList(),
        watchedSeconds = watchedSeconds,
        watchStatus = watchStatus,
    )
}

package com.filmax.feature.player.common

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.common.getOrNull
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeCatalogRepository
import com.filmax.data.fake.FakeIds
import com.filmax.data.fake.FakeLatency
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Вкладки нижней панели и сезоны в ней — чистые функции модели. Сериал — «Хроники Пограничья»
 * (7 сезонов по 12–24 серии), фильм — hero главной.
 */
class PlayerPanelTest {

    private val catalog = FakeCatalogRepository(FakeBackend(latency = FakeLatency.None))

    @Test
    fun `series gets every tab it can choose from, movie has no episodes`() {
        val series = loadedState(item(FakeIds.LONG_SERIES), season = 4, episode = 7)
        val movie = loadedState(item(FakeIds.HERO_MOVIE))

        assertEquals(PlayerTab.entries, series.panelTabs())
        assertEquals(
            listOf(PlayerTab.Audio, PlayerTab.Subtitles, PlayerTab.Quality, PlayerTab.Speed),
            movie.panelTabs()
        )
    }

    @Test
    fun `tabs without a choice are hidden but speed always stays`() {
        val bare = loadedState(item(FakeIds.HERO_MOVIE)).copy(
            audioTracks = emptyList(),
            subtitles = listOf(SubtitleOption("Выкл", null)),
            qualities = listOf(StreamQuality("1080p", listOf("https://stream"))),
        )

        assertEquals(listOf(PlayerTab.Speed), bare.panelTabs())
    }

    @Test
    fun `panel opens on episodes and later on the tab chosen last time`() {
        val series = loadedState(item(FakeIds.LONG_SERIES), season = 4, episode = 7)

        assertEquals(PlayerTab.Episodes, series.openingTab())
        assertEquals(PlayerTab.Speed, series.copy(panelTab = PlayerTab.Speed).openingTab())
        // Вкладки прошлого раза больше нет (у фильма нет серий) — первая из доступных.
        assertEquals(
            PlayerTab.Audio,
            loadedState(item(FakeIds.HERO_MOVIE)).copy(panelTab = PlayerTab.Episodes).openingTab()
        )
    }

    @Test
    fun `seasons are sorted and the shown one follows the playing episode until another is picked`() {
        val series = loadedState(item(FakeIds.LONG_SERIES), season = 4, episode = 7)
        val seasons = playerSeasons(series.item?.tracklist.orEmpty())

        assertEquals((1..LONG_SERIES_SEASONS).toList(), seasons.map { it.number })
        assertEquals(LONG_SERIES_S4_EPISODES, seasons[3].episodes.size)
        assertEquals(seasons[3].episodes.map { it.number }.sorted(), seasons[3].episodes.map { it.number })
        assertEquals(4, series.shownSeason()?.number)
        assertEquals(2, series.copy(panelSeason = 2).shownSeason()?.number)
    }

    private fun item(id: Int): Item = runBlocking { checkNotNull(catalog.getItemDetails(id).getOrNull()) }

    /** Состояние, как его собирает модель после загрузки: трек маршрута, качества и дорожки. */
    private fun loadedState(item: Item, season: Int = -1, episode: Int = -1): PlayerState {
        val track = item.tracklist.firstOrNull { it.seasonNumber == season && it.number == episode }
            ?: item.tracklist.first()
        return PlayerState(
            loading = false,
            item = item,
            track = track,
            qualities = track.files.map { StreamQuality(it.quality, listOfNotNull(it.hls4, it.hls, it.http)) },
            audioTracks = listOf(AudioOption("1. Русский", 0), AudioOption("2. English", 1)),
            subtitles = listOf(SubtitleOption("Выкл", null), SubtitleOption("Русский", "rus")),
        )
    }

    private companion object {
        const val LONG_SERIES_SEASONS = 7
        const val LONG_SERIES_S4_EPISODES = 24
    }
}

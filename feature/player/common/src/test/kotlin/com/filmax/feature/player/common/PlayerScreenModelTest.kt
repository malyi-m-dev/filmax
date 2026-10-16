package com.filmax.feature.player.common

import androidx.lifecycle.SavedStateHandle
import com.filmax.core.domain.feedback.FeedbackPrompts
import com.filmax.core.domain.playback.PlaybackSettings
import com.filmax.core.domain.playback.PlaybackSettingsRepository
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeCatalogRepository
import com.filmax.data.fake.FakeFeedbackPromptStore
import com.filmax.data.fake.FakeIds
import com.filmax.data.fake.FakeLatency
import com.filmax.data.fake.FakeUserRepository
import com.filmax.data.fake.FakeWatchingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Плеер на фейковом data-слое: трек маршрута с сезоном, следующая серия (в том числе через границу
 * сезона), нижняя панель (пауза при открытии, возврат воспроизведения, вкладка и сезон) и выбор
 * скорости и качества. Robolectric — ради маршрута из SavedStateHandle и ExoPlayer в модели;
 * сам поток не играет: главный лупер Robolectric стоит, а плеер освобождается после теста.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayerScreenModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val backend = FakeBackend(latency = FakeLatency.None)
    private val models = mutableListOf<PlayerScreenModel>()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() {
        models.forEach { it.player.release() }
        Dispatchers.resetMain()
    }

    @Test
    fun `route episode plays with its season and the next episode follows it`() = runTest(dispatcher) {
        val state = loaded(FakeIds.LONG_SERIES, season = 4, videoId = 7).states.value
        val track = assertNotNull(state.track)
        val next = assertNotNull(state.nextTrack)

        assertEquals(4 to 7, track.seasonNumber to track.number)
        assertEquals(4 to 8, next.seasonNumber to next.number)
    }

    @Test
    fun `same episode number in another season is not mistaken for the first season`() = runTest(dispatcher) {
        val track = assertNotNull(loaded(FakeIds.LONG_SERIES, season = 3, videoId = 2).states.value.track)

        assertEquals(3 to 2, track.seasonNumber to track.number)
    }

    @Test
    fun `last episode of a season leads into the next season`() = runTest(dispatcher) {
        val next = assertNotNull(loaded(FakeIds.LONG_SERIES, season = 4, videoId = LAST_OF_S4).states.value.nextTrack)

        assertEquals(5 to 1, next.seasonNumber to next.number)
    }

    @Test
    fun `movie has no next episode and no episodes tab`() = runTest(dispatcher) {
        val state = loaded(FakeIds.HERO_MOVIE).states.value

        assertNull(state.nextTrack)
        assertFalse(PlayerTab.Episodes in state.panelTabs())
    }

    @Test
    fun `panel pauses the video, remembers its tab and season, and closing resumes playback`() = runTest(dispatcher) {
        val model = loaded(FakeIds.LONG_SERIES, season = 4, videoId = 7)
        assertTrue(model.player.playWhenReady, "после загрузки видео должно играть")

        model.send(PlayerEvent.OpenPanel)
        assertTrue(model.states.value.panelOpen)
        assertEquals(PlayerTab.Episodes, model.states.value.panelTab)
        assertEquals(4, model.states.value.panelSeason)
        assertFalse(model.player.playWhenReady, "открытая панель должна поставить видео на паузу")

        model.send(PlayerEvent.ShowSeason(2))
        model.send(PlayerEvent.SelectTab(PlayerTab.Speed))
        assertEquals(2, model.states.value.shownSeason()?.number)

        model.send(PlayerEvent.ClosePanel)
        assertFalse(model.states.value.panelOpen)
        assertTrue(model.player.playWhenReady, "закрытие панели должно вернуть воспроизведение")

        model.send(PlayerEvent.OpenPanel)
        assertEquals(PlayerTab.Speed, model.states.value.panelTab, "панель открывается на вкладке прошлого раза")
        assertEquals(4, model.states.value.panelSeason, "сезон сбрасывается на сезон играющей серии")
    }

    @Test
    fun `panel opened on a paused video leaves it paused`() = runTest(dispatcher) {
        val model = loaded(FakeIds.LONG_SERIES, season = 4, videoId = 7)
        model.player.pause()

        model.send(PlayerEvent.OpenPanel)
        model.send(PlayerEvent.ClosePanel)

        assertFalse(model.player.playWhenReady)
    }

    @Test
    fun `speed and quality chosen in the panel reach the player and the state`() = runTest(dispatcher) {
        val model = loaded(FakeIds.LONG_SERIES, season = 4, videoId = 7)
        val other = model.states.value.qualities.last().label

        model.send(PlayerEvent.SetSpeed(FAST))
        model.send(PlayerEvent.SelectQuality(other))

        assertEquals(FAST, model.states.value.currentSpeed)
        assertEquals(FAST, model.player.playbackParameters.speed)
        assertEquals(other, model.states.value.currentQuality)
    }

    @Test
    fun `4K title starts in 4K while the decoder has not failed on it`() = runTest(dispatcher) {
        assertEquals("2160p", loaded(FakeIds.HERO_MOVIE).states.value.currentQuality)
    }

    @Test
    fun `device whose decoder failed on 4K starts the next title in 1080p`() = runTest(dispatcher) {
        val settings = MemoryPlaybackSettings().apply { limit = UHD_HEIGHT }

        assertEquals("1080p", loaded(FakeIds.HERO_MOVIE, settings = settings).states.value.currentQuality)
    }

    @Test
    fun `mp4 with every voice backs up hls4 before single-voice hls`() = runTest(dispatcher) {
        val state = loaded(FakeIds.HERO_MOVIE).states.value
        val file = assertNotNull(state.track).files.first()

        val urls = state.qualities.first { it.label == file.quality }.urls

        assertEquals(listOf(file.hls4, file.http, file.hls), urls)
    }

    @Test
    fun `variant that sends no video in time gives way to the backup`() = runTest(dispatcher) {
        val model = created(FakeIds.HERO_MOVIE)
        runCurrent()
        val state = model.states.value
        val urls = state.qualities.first { it.label == state.currentQuality }.urls
        assertEquals(urls[0], state.streamUrl)

        // Плеер Robolectric ничего не грузит — как заблокированный по SNI хост.
        advanceTimeBy(START_TIMEOUT_MS + 1)
        runCurrent()

        assertEquals(urls[1], model.states.value.streamUrl, "молчащий вариант должен смениться запасным")
        assertTrue(model.player.playWhenReady, "смена варианта не ставит видео на паузу")
    }

    @Test
    fun `Full HD limit left by a busy decoder does not hold the device at 720p`() = runTest(dispatcher) {
        val settings = MemoryPlaybackSettings().apply { limit = FULL_HD_CROPPED }

        assertEquals("2160p", loaded(FakeIds.HERO_MOVIE, settings = settings).states.value.currentQuality)
    }

    @Test
    fun `loading finished while the screen is hidden leaves the video paused`() = runTest(dispatcher) {
        val model = loaded(FakeIds.HERO_MOVIE) { dispatch(PlayerEvent.ScreenHidden) }

        assertNotNull(model.states.value.streamUrl, "поток должен быть выбран и подготовлен")
        assertFalse(model.player.playWhenReady, "скрытый плеер не должен играть за кадром")
    }

    private fun TestScope.loaded(
        itemId: Int,
        season: Int = -1,
        videoId: Int = -1,
        settings: MemoryPlaybackSettings = MemoryPlaybackSettings(),
        beforeLoad: PlayerScreenModel.() -> Unit = {},
    ): PlayerScreenModel {
        val model = created(itemId, season, videoId, settings)
        model.beforeLoad()
        advanceUntilIdle()
        return model
    }

    /** Модель до загрузки: корутины ещё не шли — время в тесте ведут вручную. */
    private fun created(
        itemId: Int,
        season: Int = -1,
        videoId: Int = -1,
        settings: MemoryPlaybackSettings = MemoryPlaybackSettings(),
    ): PlayerScreenModel {
        val model = PlayerScreenModel(
            savedStateHandle = SavedStateHandle(mapOf("itemId" to itemId, "videoId" to videoId, "season" to season)),
            repositories = PlayerRepositories(
                catalog = FakeCatalogRepository(backend),
                watching = FakeWatchingRepository(backend),
                user = FakeUserRepository(backend),
            ),
            playbackSettings = settings,
            feedbackPrompts = FeedbackPrompts(FakeFeedbackPromptStore(), appVersion = "test", now = { 0L }),
            context = RuntimeEnvironment.getApplication(),
        )
        models += model
        return model
    }

    private fun PlayerScreenModel.send(event: PlayerEvent) {
        dispatch(event)
        dispatcher.scheduler.advanceUntilIdle()
    }

    /** Настройки воспроизведения в памяти: дефолты и озвучки по тайтлам. */
    private class MemoryPlaybackSettings : PlaybackSettingsRepository {
        override val settings = MutableStateFlow(PlaybackSettings())
        private val voices = mutableMapOf<Int, String>()

        override suspend fun setQuality(quality: String) = settings.emit(settings.value.copy(quality = quality))

        override suspend fun setAudioLanguage(language: String) =
            settings.emit(settings.value.copy(audioLanguage = language))

        override suspend fun setSubtitleLanguage(language: String) =
            settings.emit(settings.value.copy(subtitleLanguage = language))

        override suspend fun voiceKeyFor(itemId: Int): String? = voices[itemId]

        override suspend fun setVoiceKey(itemId: Int, key: String) {
            voices[itemId] = key
        }

        var limit: Int? = null

        override suspend fun decoderLimit(): Int? = limit

        override suspend fun setDecoderLimit(height: Int?) {
            limit = height
        }
    }

    private companion object {
        /** В четвёртом сезоне «Хроник Пограничья» 24 серии. */
        const val LAST_OF_S4 = 24
        const val FAST = 1.5f
        const val UHD_HEIGHT = 2160

        /** 1920×1072 — высота части 1080p kino.pub, на которой 2.2.0 записывал ложный потолок. */
        const val FULL_HD_CROPPED = 1072
    }
}

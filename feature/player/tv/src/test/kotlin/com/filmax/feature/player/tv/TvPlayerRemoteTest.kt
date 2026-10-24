package com.filmax.feature.player.tv

import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.media3.exoplayer.ExoPlayer
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.testing.FakeImagesRule
import com.filmax.core.tv.designsystem.FilmaxTvTheme
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeCatalogRepository
import com.filmax.data.fake.FakeIds
import com.filmax.data.fake.FakeLatency
import com.filmax.feature.player.common.AudioOption
import com.filmax.feature.player.common.PlayerEvent
import com.filmax.feature.player.common.PlayerState
import com.filmax.feature.player.common.PlayerTab
import com.filmax.feature.player.common.StreamQuality
import com.filmax.feature.player.common.SubtitleOption
import com.filmax.feature.player.common.openingTab
import com.filmax.feature.player.common.tabValue
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Плеер с пульта, без видео: кадр — пустой Box, состояние собирается из «Хроник Пограничья»
 * (S4E7 играет), события модели тест применяет к состоянию так же, как модель. Проверяется
 * транспорт (фокус на паузе, перемотка, «Далее») и нижняя панель: открытие по «вниз», вкладки,
 * выбор варианта без закрытия, серия другого сезона, «вверх» со вкладок и «Назад».
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TvPlayerRemoteTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val images = FakeImagesRule()

    private val events = mutableListOf<PlayerEvent>()
    private val calls = mutableListOf<String>()
    private lateinit var player: ExoPlayer
    private lateinit var transport: TvTransportState
    private lateinit var series: Item
    private lateinit var playing: MediaTrack
    private var state by mutableStateOf(PlayerState())

    @Before
    fun setUp() {
        shadowOf(RuntimeEnvironment.getApplication().packageManager)
            .setSystemFeature(PackageManager.FEATURE_LEANBACK, true)
        series = runBlocking {
            val catalog = FakeCatalogRepository(FakeBackend(latency = FakeLatency.None))
            checkNotNull(catalog.getItemDetails(FakeIds.LONG_SERIES).getOrNull())
        }
        playing = series.tracklist.first { it.seasonNumber == 4 && it.number == 7 }
        state = loadedState()
        compose.runOnUiThread {
            player = ExoPlayer.Builder(compose.activity).build()
            transport = TvTransportState(player).apply {
                durationMs = DURATION_MS
                positionMs = POSITION_MS
            }
        }
        compose.setContent {
            FilmaxTvTheme {
                PlayerContent(PlayerScene(state, transport, error = null), actions()) { Box {} }
            }
        }
        compose.waitForIdle()
    }

    @After
    fun tearDown() {
        compose.runOnUiThread { player.release() }
    }

    @Test
    fun `player opens with focus on pause and arrows seek instead of moving focus`() {
        assertFocusedHas(PLAY)

        pressKey(Key.DirectionLeft)

        assertFocusedHas(PLAY)
        assertTrue(transport.isScrubbing)
        assertEquals(POSITION_MS - TEN_SECONDS_MS, transport.scrubTargetMs)
    }

    @Test
    fun `down opens the panel on the playing episode`() {
        pressKey(Key.DirectionDown)

        assertEquals(PlayerEvent.OpenPanel, events.last())
        assertTrue(state.panelOpen)
        assertFocusedHas(episodeTitle(playing))

        // В панели стрелки ходят по фокусу, а не перематывают.
        pressKey(Key.DirectionRight)
        assertFalse(transport.isScrubbing)
        assertFocusedHas(episodeTitle(checkNotNull(state.nextTrack)))
    }

    @Test
    fun `focus stays inside the open panel instead of falling onto the hidden pause button`() {
        pressKey(Key.DirectionDown)

        repeat(TRAP_PRESSES) { pressKey(Key.DirectionDown) }

        assertTrue(state.panelOpen)
        assertFocusedHas(episodeTitle(playing))
    }

    @Test
    fun `tabs switch by focus and a choice applies without closing the panel`() {
        pressKey(Key.DirectionDown)
        goUpTo(EPISODES_TAB)

        pressKey(Key.DirectionRight)
        assertEquals(PlayerTab.Audio, state.panelTab)
        pressKey(Key.DirectionDown)
        assertFocusedHas(RUSSIAN_DUB)
        pressKey(Key.DirectionRight)
        pressKey(Key.DirectionCenter)

        assertEquals(PlayerEvent.SelectAudio(ENGLISH_LABEL), events.last())
        assertTrue(state.panelOpen, "выбор дорожки не должен закрывать панель")
    }

    @Test
    fun `speed tile sets the speed`() {
        pressKey(Key.DirectionDown)
        goUpTo(EPISODES_TAB)
        repeat(SPEED_TAB_INDEX) { pressKey(Key.DirectionRight) }
        assertEquals(PlayerTab.Speed, state.panelTab)

        pressKey(Key.DirectionDown)
        assertFocusedHas("1×")
        pressKey(Key.DirectionRight)
        pressKey(Key.DirectionCenter)

        assertEquals(PlayerEvent.SetSpeed(FASTER), events.last())
        assertEquals("1.25×", tabValue(PlayerTab.Speed, state))
    }

    @Test
    fun `episode of another season starts with its season`() {
        pressKey(Key.DirectionDown)
        pressKey(Key.DirectionUp)
        assertFocusedHas("4")

        pressKey(Key.DirectionRight)
        assertEquals(5, state.panelSeason)
        pressKey(Key.DirectionDown)
        pressKey(Key.DirectionCenter)

        assertEquals("play 5/1", calls.last())
    }

    @Test
    fun `up from the tabs and back both close the panel and return focus to pause`() {
        pressKey(Key.DirectionDown)
        goUpTo(EPISODES_TAB)
        pressKey(Key.DirectionUp)
        assertFalse(state.panelOpen, "«вверх» со вкладок не закрыл панель")
        assertFocusedHas(PLAY)

        pressKey(Key.DirectionDown)
        pressBack()

        assertFalse(state.panelOpen, "«Назад» не закрыл панель")
        assertFocusedHas(PLAY)
    }

    @Test
    fun `ok on the next card plays the next episode with its season`() {
        showNextCard()

        pressKey(Key.DirectionCenter)

        assertEquals("play 4/8", calls.last())
    }

    @Test
    fun `back dismisses the next card instead of leaving the player`() {
        showNextCard()

        pressBack()

        assertTrue(transport.autoNext.dismissed)
        assertFalse("exit" in calls, "«Назад» при карточке «Далее» вышел из плеера")
        pressBack()
        assertEquals("exit", calls.last())
    }

    private fun showNextCard() {
        compose.runOnUiThread { transport.autoNext = AutoNext(visible = true, secondsLeft = AUTO_NEXT_COUNTDOWN_SEC) }
        compose.waitForIdle()
    }

    private fun pressBack() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    private fun loadedState() = PlayerState(
        loading = false,
        item = series,
        track = playing,
        nextTrack = series.tracklist.first { it.seasonNumber == 4 && it.number == 8 },
        qualities = listOf(StreamQuality("1080p", listOf("a")), StreamQuality("720p", listOf("b"))),
        currentQuality = "1080p",
        audioTracks = listOf(
            AudioOption(RUSSIAN_LABEL, 0, "rus", "Дубляж", "Северный дубляж", channels = 6),
            AudioOption(ENGLISH_LABEL, 1, "eng", "Оригинал", channels = 2),
        ),
        currentAudio = RUSSIAN_LABEL,
        subtitles = listOf(SubtitleOption("Выкл", null), SubtitleOption("Русский", "rus")),
        currentSubtitle = "Выкл",
    )

    /** События модели — к состоянию, как их применяет PlayerScreenModel. */
    private fun dispatch(event: PlayerEvent) {
        events += event
        state = when (event) {
            PlayerEvent.OpenPanel ->
                state.copy(panelOpen = true, panelTab = state.openingTab(), panelSeason = state.track?.seasonNumber)
            PlayerEvent.ClosePanel -> state.copy(panelOpen = false)
            is PlayerEvent.SelectTab -> state.copy(panelTab = event.tab)
            is PlayerEvent.ShowSeason -> state.copy(panelSeason = event.season)
            is PlayerEvent.SetSpeed -> state.copy(currentSpeed = event.speed)
            is PlayerEvent.SelectAudio -> state.copy(currentAudio = event.label)
            else -> state
        }
    }

    private fun actions() = PlayerActions(
        dispatch = ::dispatch,
        onPlayEpisode = { season, videoId -> calls += "play $season/$videoId" },
        onExit = { calls += "exit" },
    )

    /** «Вверх», пока фокус не встанет на [text] — потолок с запасом на ряд сезонов. */
    private fun goUpTo(text: String) {
        repeat(MAX_PRESSES) { if (!focusedHas(text)) pressKey(Key.DirectionUp) }
        assertFocusedHas(text)
    }

    private fun focusedMatcher(text: String): SemanticsMatcher {
        val labelled = hasText(text, substring = true) or hasContentDescription(text, substring = true)
        return isFocused() and (labelled or hasAnyDescendant(labelled))
    }

    private fun focusedHas(text: String): Boolean =
        compose.onAllNodes(focusedMatcher(text)).fetchSemanticsNodes().isNotEmpty()

    private fun assertFocusedHas(text: String) {
        assertTrue(focusedHas(text), "в фокусе не «$text», а ${focusedTexts()}")
    }

    /** Тексты узла в фокусе — для сообщения о провале: куда фокус ушёл на самом деле. */
    private fun focusedTexts(): List<String> = compose.onAllNodes(isFocused(), useUnmergedTree = false)
        .fetchSemanticsNodes()
        .flatMap { node ->
            node.config.getOrElse(SemanticsProperties.Text) { emptyList() }.map { it.text } +
                node.config.getOrElse(SemanticsProperties.ContentDescription) { emptyList() }
        }

    @OptIn(ExperimentalTestApi::class)
    private fun pressKey(key: Key) {
        compose.onRoot().performKeyInput { pressKey(key) }
        compose.waitForIdle()
    }

    private companion object {
        const val PLAY = "Смотреть"
        const val EPISODES_TAB = "Серии"
        const val RUSSIAN_LABEL = "1. Русский · Дубляж · Северный дубляж"
        const val ENGLISH_LABEL = "2. English · Оригинал"
        const val RUSSIAN_DUB = "Русский · Дубляж"
        const val SPEED_TAB_INDEX = 4
        const val FASTER = 1.25f
        const val MAX_PRESSES = 6
        const val TRAP_PRESSES = 3
        const val DURATION_MS = 3_398_000L
        const val POSITION_MS = 2_090_000L
        const val TEN_SECONDS_MS = 10_000L
    }
}

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
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.common.getOrNull
import com.filmax.core.testing.FakeImagesRule
import com.filmax.core.tv.designsystem.FilmaxTvTheme
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeCatalogRepository
import com.filmax.data.fake.FakeIds
import com.filmax.data.fake.FakeLatency
import com.filmax.feature.player.common.AUTO_NEXT_COUNTDOWN_SEC
import com.filmax.feature.player.common.AudioOption
import com.filmax.feature.player.common.AutoNext
import com.filmax.feature.player.common.PlaybackSnapshot
import com.filmax.feature.player.common.PlayerEvent
import com.filmax.feature.player.common.PlayerState
import com.filmax.feature.player.common.PlayerTab
import com.filmax.feature.player.common.StreamQuality
import com.filmax.feature.player.common.SubtitleOption
import com.filmax.feature.player.common.episodeTitle
import com.filmax.feature.player.common.seekStepSeconds
import com.filmax.feature.player.common.tabValue
import kotlinx.coroutines.runBlocking
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
        compose.setContent {
            FilmaxTvTheme {
                PlayerContent(state, error = null, onEvent = ::dispatch) { Box {} }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `player opens with focus on pause and arrows seek instead of moving focus`() {
        assertFocusedHas(PLAY)

        pressKey(Key.DirectionLeft)

        assertFocusedHas(PLAY)
        assertEquals(POSITION_MS - TEN_SECONDS_MS, state.scrubTargetMs)
    }

    @Test
    fun `down opens the panel on the playing episode`() {
        pressKey(Key.DirectionDown)

        assertEquals(PlayerEvent.OpenPanel, events.last())
        assertTrue(state.panelOpen)
        assertFocusedHas(episodeTitle(playing))

        // В панели стрелки ходят по фокусу, а не перематывают.
        pressKey(Key.DirectionRight)
        assertEquals(null, state.scrubTargetMs)
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

        assertEquals(PlayerEvent.PlayEpisode(season = 5, videoId = 1), events.last())
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
    fun `ok on the next card plays the next episode`() {
        showNextCard()

        pressKey(Key.DirectionCenter)

        assertEquals(PlayerEvent.PlayNext, events.last())
    }

    @Test
    fun `back dismisses the next card instead of leaving the player`() {
        showNextCard()

        pressBack()

        assertEquals(PlayerEvent.DismissAutoNext, events.last())
        assertTrue(state.autoNext.dismissed)
        pressBack()
        assertEquals(PlayerEvent.Exit, events.last())
    }

    private fun showNextCard() {
        compose.runOnUiThread {
            state = state.copy(autoNext = AutoNext(visible = true, secondsLeft = AUTO_NEXT_COUNTDOWN_SEC))
        }
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
        // Видео стоит: иначе контролы ушли бы по таймеру, а кнопка звалась бы «Пауза».
        playback = PlaybackSnapshot(playing = false, positionMs = POSITION_MS, durationMs = DURATION_MS),
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
            is PlayerEvent.SeekStep -> state.copy(scrubTargetMs = scrubbed(event))
            PlayerEvent.DismissAutoNext -> state.copy(autoNext = state.autoNext.copy(visible = false, dismissed = true))
            else -> state
        }
    }

    /** Шаг перемотки, как его считает модель: от текущей цели или позиции плеера. */
    private fun scrubbed(step: PlayerEvent.SeekStep): Long {
        val from = state.scrubTargetMs ?: state.playback.positionMs
        return from + step.direction * seekStepSeconds(0) * MILLIS_IN_SECOND
    }

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
        const val MILLIS_IN_SECOND = 1000L
    }
}

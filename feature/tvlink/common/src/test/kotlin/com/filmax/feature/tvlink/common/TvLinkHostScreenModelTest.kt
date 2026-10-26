package com.filmax.feature.tvlink.common

import com.filmax.core.domain.tvlink.PlaybackStatus
import com.filmax.core.domain.tvlink.RemoteCommand
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.NavCommand
import com.filmax.data.fake.FakeIds
import com.filmax.data.fake.FakeNavigator
import com.filmax.data.fake.FakeTvLinkReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Приёмник на ТВ: виден в сети только на экране, открывает плеер на тайтл с телефона, объявляет телефоны. */
@OptIn(ExperimentalCoroutinesApi::class)
class TvLinkHostScreenModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val receiver = FakeTvLinkReceiver()
    private val navigator = FakeNavigator()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `tv listens only while filmax is on screen`() = runTest(dispatcher) {
        val model = host()

        model.dispatch(TvLinkHostEvent.Shown)
        runCurrent()
        assertTrue(receiver.state.value.listening)

        model.dispatch(TvLinkHostEvent.Hidden)
        runCurrent()
        assertFalse(receiver.state.value.listening)
    }

    @Test
    fun `title from the phone opens the player, and replaces the one already playing`() = runTest(dispatcher) {
        host()

        receiver.receive(PLAY)
        runCurrent()
        receiver.publish(PLAYING)
        receiver.receive(PLAY)
        runCurrent()

        val player = Destination.Player(PLAY.itemId, PLAY.videoId, PLAY.season, PLAY.startSeconds)
        assertEquals(listOf(NavCommand.Open(player), NavCommand.Replace(player)), navigator.sent)
    }

    @Test
    fun `transport commands are left to the player`() = runTest(dispatcher) {
        host()

        receiver.receive(RemoteCommand.Pause)
        runCurrent()

        assertTrue(navigator.sent.isEmpty())
    }

    @Test
    fun `connected phone is announced for a few seconds`() = runTest(dispatcher) {
        val model = host()

        receiver.connectPhone("Galaxy S25")
        runCurrent()
        assertEquals("Galaxy S25", model.states.value.joinedPhone)
        assertEquals("Galaxy S25", model.states.value.lastJoinedPhone)

        advanceTimeBy(ANNOUNCE_MS + 1)
        runCurrent()
        assertNull(model.states.value.joinedPhone)
    }

    private fun TestScope.host(): TvLinkHostScreenModel {
        val model = TvLinkHostScreenModel(receiver, navigator)
        runCurrent()
        return model
    }

    private companion object {
        const val ANNOUNCE_MS = 4_000L
        val PLAY = RemoteCommand.Play(itemId = FakeIds.HERO_MOVIE, videoId = -1, season = -1, startSeconds = 1_250)
        val PLAYING = PlaybackStatus(
            itemId = FakeIds.MOVIE_IN_PROGRESS,
            title = "Код доступа: Гелиос",
            posterUrl = null,
            season = -1,
            episode = 1,
            episodeTitle = null,
            playing = true,
            buffering = false,
            positionMs = 420_000,
            durationMs = 6_000_000,
            hasNext = false,
            hasPrevious = false,
        )
    }
}

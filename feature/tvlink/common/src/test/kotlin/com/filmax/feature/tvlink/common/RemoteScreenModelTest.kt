package com.filmax.feature.tvlink.common

import com.filmax.core.domain.tvlink.PlaybackStatus
import com.filmax.core.domain.tvlink.RemoteCommand
import com.filmax.core.domain.tvlink.RemoteSession
import com.filmax.core.domain.tvlink.TvDevice
import com.filmax.data.fake.FakeIds
import com.filmax.data.fake.FakeTvRemote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/** Пульт: команды по состоянию плеера на ТВ, ползунок под пальцем, обрыв связи и отключение. */
@OptIn(ExperimentalCoroutinesApi::class)
class RemoteScreenModelTest {

    private val remote = FakeTvRemote().apply { reachable += LIVING_ROOM.id }
    private val effects = mutableListOf<RemoteSideEffect>()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `play button pauses what plays and resumes what is paused`() = runTest {
        val model = connected(STATUS)

        model.dispatch(RemoteEvent.TogglePlay)
        remote.showPlayback(STATUS.copy(playing = false))
        model.dispatch(RemoteEvent.TogglePlay)

        assertEquals(listOf(RemoteCommand.Pause, RemoteCommand.Resume), remote.sent)
    }

    @Test
    fun `slider follows the finger and seeks once on release`() = runTest {
        val model = connected(STATUS)

        model.dispatch(RemoteEvent.Scrub(1_000_000))
        remote.showPlayback(STATUS.copy(positionMs = STATUS.positionMs + 1_000))
        assertEquals(1_000_000, model.states.value.positionMs, "снимок с ТВ не должен дёргать ползунок под пальцем")

        model.dispatch(RemoteEvent.ScrubFinished)

        assertEquals(listOf<RemoteCommand>(RemoteCommand.SeekTo(1_000_000)), remote.sent)
        assertNull(model.states.value.scrubPositionMs)
    }

    @Test
    fun `dropped link reconnects to the same tv`() = runTest {
        val model = connected(STATUS)
        remote.drop()
        assertIs<RemoteSession.Lost>(model.states.value.session)

        model.dispatch(RemoteEvent.Reconnect)

        assertEquals(LIVING_ROOM, assertIs<RemoteSession.Connected>(model.states.value.session).device)
    }

    @Test
    fun `nothing playing on the tv means no play command`() = runTest {
        val model = connected(status = null)

        model.dispatch(RemoteEvent.TogglePlay)

        assertEquals(emptyList(), remote.sent)
    }

    @Test
    fun `disconnect closes the remote`() = runTest {
        val model = connected(STATUS)

        model.dispatch(RemoteEvent.Disconnect)

        assertEquals(RemoteSession.Idle, model.states.value.session)
        assertEquals(listOf<RemoteSideEffect>(RemoteSideEffect.Close), effects)
    }

    private suspend fun connected(status: PlaybackStatus?): RemoteScreenModel {
        remote.connect(LIVING_ROOM)
        remote.showPlayback(status)
        return RemoteScreenModel(remote).also { model -> model.observeSideEffects { effects += it } }
    }

    private companion object {
        val LIVING_ROOM = TvDevice(id = "Гостиная", name = "Гостиная", host = "192.168.1.50", port = 47100)
        val STATUS = PlaybackStatus(
            itemId = FakeIds.LONG_SERIES,
            title = "Хроники Пограничья",
            posterUrl = null,
            season = 4,
            episode = 7,
            episodeTitle = null,
            playing = true,
            buffering = false,
            positionMs = 60_000,
            durationMs = 2_700_000,
            hasNext = true,
            hasPrevious = true,
        )
    }
}

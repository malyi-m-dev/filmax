package com.filmax.feature.tvlink.common

import androidx.lifecycle.SavedStateHandle
import com.filmax.core.domain.tvlink.LinkFailure
import com.filmax.core.domain.tvlink.RemoteCommand
import com.filmax.core.domain.tvlink.TvDevice
import com.filmax.core.navigation.Destination
import com.filmax.core.navigation.NavCommand
import com.filmax.data.fake.FakeIds
import com.filmax.data.fake.FakeNavigator
import com.filmax.data.fake.FakeTvDiscovery
import com.filmax.data.fake.FakeTvRemote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Выбор телевизора: свои ТВ из поиска, адрес вручную, отказ чужого аккаунта и передача тайтла. */
@OptIn(ExperimentalCoroutinesApi::class)
// Маршрут из SavedStateHandle (toRoute) читается через Bundle — нужен Android-рантайм.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TvPickerScreenModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val discovery = FakeTvDiscovery()
    private val remote = FakeTvRemote()
    private val navigator = FakeNavigator()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `chosen tv gets the title and the remote opens`() = runTest(dispatcher) {
        discovery.found.value = listOf(LIVING_ROOM)
        remote.reachable += LIVING_ROOM.id
        val model = opened(withTitle = true)
        assertEquals(listOf(LIVING_ROOM), model.states.value.devices)

        model.send(TvPickerEvent.Choose(LIVING_ROOM.id))

        assertEquals(listOf<RemoteCommand>(PLAY), remote.sent)
        assertEquals(listOf<NavCommand>(NavCommand.Replace(Destination.Remote)), navigator.sent)
    }

    @Test
    fun `tv of another account is refused and nothing is sent`() = runTest(dispatcher) {
        discovery.found.value = listOf(LIVING_ROOM)
        remote.otherAccount += LIVING_ROOM.id
        val model = opened(withTitle = true)

        model.send(TvPickerEvent.Choose(LIVING_ROOM.id))

        assertEquals(TvPickerFailure("Гостиная", LinkFailure.OtherAccount), model.states.value.failure)
        assertTrue(model.states.value.failure?.message.orEmpty().contains("другой аккаунт"))
        assertNull(model.states.value.connectingTo, "после отказа можно выбрать другой ТВ")
        assertTrue(remote.sent.isEmpty())
        assertTrue(navigator.sent.isEmpty())
    }

    @Test
    fun `address typed by hand connects when the router hides the tv`() = runTest(dispatcher) {
        remote.reachable += "192.168.1.50"
        val model = opened(withTitle = true)

        model.send(TvPickerEvent.OpenManual)
        model.send(TvPickerEvent.EditAddress("гостиная"))
        model.send(TvPickerEvent.ConnectByAddress)
        assertTrue(model.states.value.addressInvalid)

        model.send(TvPickerEvent.EditAddress("192.168.1.50"))
        assertFalse(model.states.value.addressInvalid, "правка адреса снимает ошибку")
        model.send(TvPickerEvent.ConnectByAddress)

        assertEquals(listOf<RemoteCommand>(PLAY), remote.sent)
        assertEquals(listOf<NavCommand>(NavCommand.Replace(Destination.Remote)), navigator.sent)
    }

    @Test
    fun `tv chosen for the remote gets no title`() = runTest(dispatcher) {
        discovery.found.value = listOf(LIVING_ROOM)
        remote.reachable += LIVING_ROOM.id
        val model = opened(withTitle = false)
        assertFalse(model.states.value.withTitle)

        model.send(TvPickerEvent.Choose(LIVING_ROOM.id))

        assertTrue(remote.sent.isEmpty())
        assertEquals(listOf<NavCommand>(NavCommand.Replace(Destination.Remote)), navigator.sent)
    }

    @Test
    fun `connected tv gets the title right away without reconnecting`() = runTest(dispatcher) {
        discovery.found.value = listOf(LIVING_ROOM)
        remote.reachable += LIVING_ROOM.id
        remote.connect(LIVING_ROOM)
        // Связь уже есть: если бы модель переподключалась, отказ здесь сорвал бы передачу.
        remote.reachable.clear()

        opened(withTitle = true)

        assertEquals(listOf<RemoteCommand>(PLAY), remote.sent)
        assertEquals(listOf<NavCommand>(NavCommand.Replace(Destination.Remote)), navigator.sent)
    }

    @Test
    fun `remembered tv gets the title without choosing even when search misses it`() = runTest(dispatcher) {
        remote.pair(LIVING_ROOM)
        remote.reachable += LIVING_ROOM.id

        opened(withTitle = true)

        assertEquals(listOf(LIVING_ROOM), remote.connects)
        assertEquals(listOf<RemoteCommand>(PLAY), remote.sent)
        assertEquals(listOf<NavCommand>(NavCommand.Replace(Destination.Remote)), navigator.sent)
    }

    @Test
    fun `remembered tv that just failed is listed first and not dialed again`() = runTest(dispatcher) {
        val bedroom = TvDevice(id = "tv-bedroom", name = "Спальня", host = "192.168.1.51", port = 47100)
        discovery.found.value = listOf(bedroom)
        remote.pair(LIVING_ROOM)
        remote.connect(LIVING_ROOM)
        remote.connects.clear()

        val model = opened(withTitle = true)

        assertEquals(listOf(LIVING_ROOM, bedroom), model.states.value.devices)
        assertTrue(remote.connects.isEmpty(), "выключенный ТВ не дёргаем — выбор за зрителем")
        assertTrue(navigator.sent.isEmpty())
    }

    @Test
    fun `long search explains where the tv may be`() = runTest(dispatcher) {
        val model = opened(withTitle = true)
        assertFalse(model.states.value.searchTakesLong)

        advanceTimeBy(SLOW_SEARCH_MS + 1)
        runCurrent()

        assertTrue(model.states.value.searchTakesLong)
    }

    private fun TestScope.opened(withTitle: Boolean): TvPickerScreenModel {
        val route = mapOf(
            "itemId" to PLAY.itemId,
            "videoId" to PLAY.videoId,
            "season" to PLAY.season,
            "startSeconds" to PLAY.startSeconds,
        )
        val handle = SavedStateHandle(if (withTitle) route else emptyMap())
        val model = TvPickerScreenModel(handle, discovery, remote, navigator)
        runCurrent()
        return model
    }

    private fun TvPickerScreenModel.send(event: TvPickerEvent) {
        dispatch(event)
        dispatcher.scheduler.runCurrent()
    }

    private companion object {
        val LIVING_ROOM = TvDevice(id = "Гостиная", name = "Гостиная", host = "192.168.1.50", port = 47100)
        val PLAY = RemoteCommand.Play(itemId = FakeIds.LONG_SERIES, videoId = 7, season = 4, startSeconds = 754)
        const val SLOW_SEARCH_MS = 4_000L
    }
}

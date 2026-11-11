package com.filmax.feature.onboarding.common

import com.filmax.core.domain.feedback.ClientInfo
import com.filmax.core.domain.source.ContentSource
import com.filmax.core.presentation.input.KeyboardLayout
import com.filmax.data.fake.FakeContentSourceRepository
import com.filmax.data.fake.FakeHomeServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Вход на домашний сервер на фейках: сервер из сети подставляется сам, ТВ входит по коду,
 * телефон — паролем, ошибка пароля объясняется по-человечески, а после входа источник — сервер.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeServerScreenModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val servers = FakeHomeServerRepository()
    private val sources = FakeContentSourceRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the only server in the network fills the address`() = runTest(dispatcher) {
        val model = model(tv = true)
        advanceUntilIdle()

        assertEquals(FakeHomeServerRepository.HOME.address, model.states.value.address)
        assertEquals(listOf(FakeHomeServerRepository.HOME), model.states.value.found)
    }

    @Test
    fun `tv signs in with a quick connect code`() = runTest(dispatcher) {
        val model = model(tv = true)
        advanceUntilIdle()

        model.dispatch(HomeServerEvent.Choose(FakeHomeServerRepository.HOME))
        dispatcher.scheduler.runCurrent()
        assertEquals(HomeServerStep.CODE, model.states.value.step)
        advanceUntilIdle()

        assertEquals("482913", model.states.value.code)
        assertTrue(servers.signedIn)
        assertEquals(ContentSource.JELLYFIN, sources.source.value)
    }

    @Test
    fun `phone signs in with a password`() = runTest(dispatcher) {
        val model = model(tv = false)
        advanceUntilIdle()
        model.dispatch(HomeServerEvent.Connect)
        advanceUntilIdle()
        assertEquals(HomeServerStep.PASSWORD, model.states.value.step)

        model.dispatch(HomeServerEvent.SetUsername("anna "))
        model.dispatch(HomeServerEvent.SetPassword("secret"))
        model.dispatch(HomeServerEvent.SignIn)
        advanceUntilIdle()

        assertTrue(servers.signedIn)
        assertEquals(ContentSource.JELLYFIN, sources.source.value)
    }

    @Test
    fun `wrong password is explained and keeps the source`() = runTest(dispatcher) {
        val model = model(tv = false)
        advanceUntilIdle()
        model.dispatch(HomeServerEvent.Connect)
        advanceUntilIdle()
        model.dispatch(HomeServerEvent.SetPassword("nope"))
        model.dispatch(HomeServerEvent.SignIn)
        advanceUntilIdle()

        assertEquals("Неверное имя пользователя или пароль", model.states.value.error)
        assertEquals(ContentSource.KINOPUB, sources.source.value)
    }

    @Test
    fun `unreachable address is an offline error`() = runTest(dispatcher) {
        servers.found = emptyList()
        val model = model(tv = true)
        advanceUntilIdle()

        model.dispatch(HomeServerEvent.SetAddress("10.0.0.9"))
        model.dispatch(HomeServerEvent.Connect)
        advanceUntilIdle()

        assertEquals(HomeServerStep.SERVER, model.states.value.step)
        assertNotNull(model.states.value.error).let { assertTrue(it.startsWith("Сервер не отвечает"), it) }
    }

    @Test
    fun `tv keyboard types into the chosen field as is`() = runTest(dispatcher) {
        servers.found = emptyList()
        val model = model(tv = true)
        advanceUntilIdle()

        model.dispatch(HomeServerEvent.Key(ServerKey.Layout(KeyboardLayout.DIGITS)))
        "192.168.1.5:8096".forEach { model.dispatch(HomeServerEvent.Key(ServerKey.Letter(it.toString()))) }
        model.dispatch(HomeServerEvent.Key(ServerKey.Erase))
        model.dispatch(HomeServerEvent.Focus(HomeServerField.USERNAME))
        "anna".forEach { model.dispatch(HomeServerEvent.Key(ServerKey.Letter(it.toString()))) }
        advanceUntilIdle()

        assertEquals("192.168.1.5:809", model.states.value.address)
        assertEquals("anna", model.states.value.username)
        assertEquals(KeyboardLayout.DIGITS, model.states.value.layout)
        assertTrue(serverKeyboardRows(KeyboardLayout.DIGITS).flatten().containsAll(listOf(".", ":", "/")))
    }

    @Test
    fun `changing the server stops waiting for the code`() = runTest(dispatcher) {
        val model = model(tv = true)
        advanceUntilIdle()
        model.dispatch(HomeServerEvent.Choose(FakeHomeServerRepository.HOME))
        dispatcher.scheduler.runCurrent()

        model.dispatch(HomeServerEvent.ChangeServer)
        advanceUntilIdle()

        assertEquals(HomeServerStep.SERVER, model.states.value.step)
        assertNull(model.states.value.code)
        assertEquals(ContentSource.KINOPUB, sources.source.value)
    }

    private fun model(tv: Boolean) = HomeServerScreenModel(
        servers = servers,
        sources = sources,
        client = ClientInfo(appVersion = "2.8.0", isTv = tv, device = "Test", os = "Android 14"),
    )
}

package com.filmax.app.navigation

import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.feedback.ClientInfo
import com.filmax.core.domain.user.model.DeviceSettings
import com.filmax.data.fake.FakeAuthRepository
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeLatency
import com.filmax.data.fake.FakeUserRepository
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
import kotlin.test.assertIs

/**
 * Подпись устройства в аккаунте kino.pub шлёт корневая модель, а не экран входа: тот снимается
 * навигацией сразу после сохранения токена и отменял запрос — устройства оставались «unknown».
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RootScreenModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val backend = FakeBackend(latency = FakeLatency.None)
    private val user = FakeUserRepository(backend)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `signed-in app describes the device in the kino pub account`() = runTest(dispatcher) {
        RootScreenModel(FakeAuthRepository(backend), user, MI_TV)
        advanceUntilIdle()

        val device = assertIs<RequestResult.Success<DeviceSettings>>(user.getDeviceSettings()).data
        assertEquals("Xiaomi MiTV-MZTU1", device.title)
    }

    private companion object {
        val MI_TV = ClientInfo(appVersion = "2.3.2", isTv = true, device = "Xiaomi MiTV-MZTU1", os = "Android 14")
    }
}

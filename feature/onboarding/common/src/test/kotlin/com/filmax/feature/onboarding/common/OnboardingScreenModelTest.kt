package com.filmax.feature.onboarding.common

import com.filmax.data.fake.FakeAuthRepository
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeLatency
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
import kotlin.test.assertTrue

/** Вход на фейковом бэкенде: «Войти» запрашивает код, подтверждение делает Главную корнем. */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingScreenModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val backend = FakeBackend(latency = FakeLatency.None)
    private val effects = mutableListOf<OnboardingSideEffect>()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `login moves to the code step and asks for a code`() = runTest(dispatcher) {
        val model = model()

        model.dispatch(OnboardingEvent.Login)
        dispatcher.scheduler.runCurrent()

        val state = model.states.value
        assertEquals(OnboardingStep.AUTH, state.step)
        assertNotNull(state.userCode, "код не запросился")
        assertTrue(state.polling)
    }

    @Test
    fun `confirmed device makes home the root`() = runTest(dispatcher) {
        val model = model()
        model.dispatch(OnboardingEvent.Login)
        advanceUntilIdle()

        assertEquals(listOf<OnboardingSideEffect>(OnboardingSideEffect.SignedIn), effects)
    }

    /** Часы модели идут вместе с планировщиком теста: иначе опрос ждал бы настоящие секунды. */
    private fun model() = OnboardingScreenModel(
        auth = FakeAuthRepository(backend),
        now = { dispatcher.scheduler.currentTime },
    ).also { it.observeSideEffects(effects::add) }
}

package com.filmax.feature.feedback.common

import androidx.lifecycle.SavedStateHandle
import com.filmax.core.domain.feedback.ClientInfo
import com.filmax.core.domain.feedback.FeedbackPrompts
import com.filmax.core.domain.feedback.FeedbackTrigger
import com.filmax.core.navigation.NavCommand
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeFeedbackPromptStore
import com.filmax.data.fake.FakeFeedbackRepository
import com.filmax.data.fake.FakeLatency
import com.filmax.data.fake.FakeNavigator
import com.filmax.data.fake.FakeUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Окно оценки на фейках: что уходит в отзыв, когда он не уходит и что запоминает политика показов. */
@OptIn(ExperimentalCoroutinesApi::class)
// Маршрут из SavedStateHandle (toRoute) читается через Bundle — нужен Android-рантайм.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FeedbackScreenModelTest {

    private val backend = FakeBackend(latency = FakeLatency.None)
    private val repository = FakeFeedbackRepository(backend)
    private val store = FakeFeedbackPromptStore()
    private val client = ClientInfo(appVersion = "2.2.0", isTv = true, device = "Xiaomi MIBOX4", os = "Android 9")
    private val navigator = FakeNavigator()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `rating, keyboard comment and device go into the feedback`() {
        val model = model(FeedbackTrigger.AfterWatch)

        model.dispatch(FeedbackEvent.Rate(4))
        listOf("О", "к").forEach { model.dispatch(FeedbackEvent.Key(FeedbackInput.Letter(it))) }
        model.dispatch(FeedbackEvent.Send)

        val sent = assertNotNull(repository.sent.singleOrNull())
        assertEquals(4, sent.rating)
        assertEquals("Ок", sent.comment)
        assertEquals(FeedbackTrigger.AfterWatch, sent.trigger)
        assertEquals(client, sent.client)
        assertTrue(model.states.value.sent)
        assertNotNull(store.state.lastSentAt, "отправка не запомнилась — окно вернётся через 60 дней")
    }

    @Test
    fun `without a rating nothing is sent and the window says why`() {
        val model = model(FeedbackTrigger.Profile)

        model.dispatch(FeedbackEvent.Send)

        assertTrue(repository.sent.isEmpty())
        assertNotNull(model.states.value.error)
    }

    @Test
    fun `failed send keeps the text and shows an error`() {
        repository.fails = true
        val model = model(FeedbackTrigger.Profile)
        model.dispatch(FeedbackEvent.Rate(2))
        model.dispatch(FeedbackEvent.Comment("Тормозит меню"))

        model.dispatch(FeedbackEvent.Send)

        assertFalse(model.states.value.sent)
        assertEquals("Тормозит меню", model.states.value.comment)
        assertNotNull(model.states.value.error)
    }

    @Test
    fun `window opened by itself counts as a prompt, opened from the profile does not`() {
        model(FeedbackTrigger.Profile)
        assertNull(store.state.lastPromptAt)

        model(FeedbackTrigger.AfterUpdate)
        assertNotNull(store.state.lastPromptAt)
    }

    @Test
    fun `dismiss closes the window`() {
        val model = model(FeedbackTrigger.Profile)
        assertEquals("Оцените Filmax", model.states.value.title)

        model.dispatch(FeedbackEvent.Dismiss)

        assertEquals(NavCommand.Back, navigator.sent.last())
    }

    private fun model(trigger: FeedbackTrigger) = FeedbackScreenModel(
        savedStateHandle = SavedStateHandle(mapOf("trigger" to trigger.name)),
        repository = repository,
        prompts = FeedbackPrompts(store, appVersion = client.appVersion, now = { NOW }),
        user = FakeUserRepository(backend),
        client = client,
        navigator = navigator,
    )

    private companion object {
        const val NOW = 1_800_000_000_000L
    }
}

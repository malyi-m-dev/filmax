package com.filmax.core.presentation

import com.filmax.core.domain.common.ErrorReporter
import com.filmax.core.domain.common.ErrorReporting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Телеметрия базовой модели: событие экрана — хлебная крошка с именем экрана, а исключение в
 * корутине экрана — отчёт, а не тишина.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BaseScreenModelTelemetryTest {

    private val recorder = RecordingReporter()
    private lateinit var previousReporter: ErrorReporter

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        previousReporter = ErrorReporting.reporter
        ErrorReporting.reporter = recorder
    }

    @AfterTest
    fun tearDown() {
        ErrorReporting.reporter = previousReporter
        Dispatchers.resetMain()
    }

    @Test
    fun `every event becomes a breadcrumb with the screen name`() {
        val model = SampleScreenModel()

        model.dispatch(SampleEvent.Toggle(folderId = 5))

        assertEquals(listOf("log: event: SampleScreenModel.Toggle(folderId=5)"), recorder.events)
        assertEquals(listOf<SampleEvent>(SampleEvent.Toggle(5)), model.handled)
    }

    @Test
    fun `periodic event is handled but not logged`() {
        val model = SampleScreenModel()

        model.dispatch(SampleEvent.Tick)

        assertTrue(recorder.events.isEmpty(), "тик попал в крошки: ${recorder.events}")
        assertEquals(listOf<SampleEvent>(SampleEvent.Tick), model.handled)
    }

    @Test
    fun `failure inside a screen coroutine is reported`() {
        val model = SampleScreenModel()

        model.dispatch(SampleEvent.Fail)

        assertTrue("report: сломалось" in recorder.events, "${recorder.events}")
    }

    private sealed interface SampleEvent {
        data class Toggle(val folderId: Int) : SampleEvent
        data object Fail : SampleEvent
        data object Tick : SampleEvent, PeriodicEvent
    }

    private class SampleScreenModel : BaseScreenModel<Unit, Unit, SampleEvent>(Unit) {
        val handled = mutableListOf<SampleEvent>()

        override fun onEvent(event: SampleEvent) {
            handled += event
            if (event == SampleEvent.Fail) screenModelScope { _ -> error("сломалось") }
        }

        override fun onFetchData() = Unit
    }

    private class RecordingReporter : ErrorReporter {
        val events = mutableListOf<String>()

        override fun setUser(id: String?) = Unit

        override fun log(message: String) {
            events += "log: $message"
        }

        override fun setKey(key: String, value: String) {
            events += "key: $key=$value"
        }

        override fun report(error: Throwable) {
            events += "report: ${error.message}"
        }
    }
}

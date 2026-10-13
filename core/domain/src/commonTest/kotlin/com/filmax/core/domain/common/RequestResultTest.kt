package com.filmax.core.domain.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RequestResultTest {

    private val recorder = RecordingReporter()
    private lateinit var previousReporter: ErrorReporter

    @BeforeTest
    fun installRecorder() {
        previousReporter = ErrorReporting.reporter
        ErrorReporting.reporter = recorder
    }

    @AfterTest
    fun restoreReporter() {
        ErrorReporting.reporter = previousReporter
    }

    @Test
    fun `safeRequest wraps the value into Success`() = runTest {
        val result = safeRequest { 42 }

        assertEquals(RequestResult.Success(42), result)
        assertTrue(recorder.events.isEmpty())
    }

    @Test
    fun `safeRequest turns a failure into Error with message and cause`() = runTest {
        val failure = IllegalStateException("сервер ответил 500")

        val result = safeRequest<Int> { throw failure }

        val error = assertIs<RequestResult.Error>(result)
        assertEquals("сервер ответил 500", error.message)
        assertSame(failure, error.cause)
    }

    @Test
    fun `safeRequest reports every failure it swallows`() = runTest {
        safeRequest<Int> { throw IllegalStateException("сервер ответил 500") }

        assertTrue(recorder.events.isNotEmpty())
    }

    @Test
    fun `safeRequest rethrows cancellation instead of turning it into Error`() = runTest {
        assertFailsWith<CancellationException> {
            safeRequest<Int> { throw CancellationException("экран закрыт") }
        }
        assertTrue(recorder.events.isEmpty())
    }

    @Test
    fun `map transforms Success and passes Error through`() {
        val error = RequestResult.Error("нет сети")

        assertEquals(RequestResult.Success("4"), RequestResult.Success(4).map { it.toString() })
        assertSame(error, error.map { value: Int -> value + 1 })
    }

    @Test
    fun `getOrNull returns data only for Success`() {
        assertEquals(4, RequestResult.Success(4).getOrNull())
        assertNull(RequestResult.Error("нет сети").getOrNull())
    }

    @Test
    fun `firstErrorMessage picks the first failed result`() {
        val message = firstErrorMessage(
            RequestResult.Success(1),
            RequestResult.Error("первая"),
            RequestResult.Error("вторая"),
        )

        assertEquals("первая", message)
        assertNull(firstErrorMessage(RequestResult.Success(1), RequestResult.Success(2)))
    }

    /** Пишет всё, что ему отдали, — и события, и хлебные крошки. */
    private class RecordingReporter : ErrorReporter {
        val events = mutableListOf<String>()

        override fun setUser(id: String?) = Unit

        override fun log(message: String) {
            events += "log: $message"
        }

        override fun report(error: Throwable) {
            events += "report: ${error.message}"
        }
    }
}

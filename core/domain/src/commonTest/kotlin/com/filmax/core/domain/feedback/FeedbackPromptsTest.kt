package com.filmax.core.domain.feedback

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeedbackPromptsTest {

    private val store = MemoryStore()
    private var time = START

    private fun prompts(version: String = "2.2.0") = FeedbackPrompts(store, version, now = { time })

    @Test
    fun `first launch after install does not ask, an update does`() = runTest {
        assertFalse(prompts("2.1.0").afterAppStart(), "спросили сразу после установки")
        assertFalse(prompts("2.1.0").afterAppStart(), "спросили при обычном запуске")

        assertTrue(prompts("2.2.0").afterAppStart(), "не спросили после обновления")
    }

    @Test
    fun `watches ask only from the third one`() = runTest {
        val prompts = prompts()

        assertFalse(prompts.afterWatchFinished())
        assertFalse(prompts.afterWatchFinished())
        assertTrue(prompts.afterWatchFinished())
    }

    @Test
    fun `after a prompt the window stays quiet for sixty days`() = runTest {
        val prompts = prompts()
        repeat(FeedbackPrompts.MIN_FINISHED_WATCHES - 1) { prompts.afterWatchFinished() }
        prompts.onPrompted()

        time += FeedbackPrompts.PROMPT_INTERVAL_MS - 1
        assertFalse(prompts.afterWatchFinished(), "спросили раньше чем через 60 дней")

        time += 1
        assertTrue(prompts.afterWatchFinished())
    }

    @Test
    fun `sent feedback silences an update prompt`() = runTest {
        prompts("2.1.0").afterAppStart()
        prompts().onSent()

        assertFalse(prompts("2.2.0").afterAppStart(), "спросили после отправленного отзыва")
    }

    private class MemoryStore : FeedbackPromptStore {
        private var state = FeedbackPromptState()

        override suspend fun load() = state

        override suspend fun save(state: FeedbackPromptState) {
            this.state = state
        }
    }

    private companion object {
        const val START = 1_800_000_000_000L
    }
}

package com.filmax.feature.player.common

import kotlin.test.Test
import kotlin.test.assertEquals

/** Прогресс уходит на сервер не каждую секунду тика, а с шагом — и сразу после перемотки. */
class ProgressThrottleTest {

    @Test
    fun `first position goes at once, the next ones every few seconds`() {
        val throttle = ProgressThrottle(stepSeconds = 5)

        val sent = (0..12).filter(throttle::shouldSend)

        assertEquals(listOf(0, 5, 10), sent)
    }

    @Test
    fun `seek back is a move too and goes at once`() {
        val throttle = ProgressThrottle(stepSeconds = 5)
        throttle.shouldSend(600)

        assertEquals(listOf(true, false), listOf(throttle.shouldSend(120), throttle.shouldSend(122)))
    }
}

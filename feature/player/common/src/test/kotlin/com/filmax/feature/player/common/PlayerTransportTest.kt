package com.filmax.feature.player.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Разгон перемотки с пульта и отсчёт карточки «Далее» — решения модели, чистые функции. */
class PlayerTransportTest {

    @Test
    fun `holding an arrow speeds seeking up 10 - 30 - 60 every two seconds`() {
        assertEquals(10, seekStepSeconds(heldMs = 0))
        assertEquals(10, seekStepSeconds(heldMs = 1_999))
        assertEquals(30, seekStepSeconds(heldMs = 2_000))
        assertEquals(30, seekStepSeconds(heldMs = 3_999))
        assertEquals(60, seekStepSeconds(heldMs = 4_000))
        assertEquals(60, seekStepSeconds(heldMs = 60_000))
    }

    @Test
    fun `next card appears 30 seconds before the end and counts 15 seconds while playing`() {
        val hidden = AutoNext().tick(remainingMs = 31_000, enabled = true, playing = true, scrubbing = false)
        assertFalse(hidden.visible)

        val shown = hidden.tick(remainingMs = 30_000, enabled = true, playing = true, scrubbing = false)
        assertTrue(shown.visible)
        assertEquals(AUTO_NEXT_COUNTDOWN_SEC, shown.secondsLeft)
        assertEquals(0f, shown.elapsed)

        val paused = shown.tick(remainingMs = 29_000, enabled = true, playing = false, scrubbing = false)
        assertEquals(AUTO_NEXT_COUNTDOWN_SEC, paused.secondsLeft, "на паузе отсчёт стоит")

        val expired = (1..AUTO_NEXT_COUNTDOWN_SEC).fold(shown) { card, _ ->
            card.tick(remainingMs = 20_000, enabled = true, playing = true, scrubbing = false)
        }
        assertTrue(expired.expired)
        assertEquals(1f, expired.elapsed)
    }

    @Test
    fun `scrubbing hides the card and a dismissed card never returns`() {
        val shown = AutoNext().tick(remainingMs = 10_000, enabled = true, playing = true, scrubbing = false)

        assertFalse(shown.tick(remainingMs = 9_000, enabled = true, playing = true, scrubbing = true).visible)
        val dismissed = shown.copy(visible = false, dismissed = true)
        assertFalse(dismissed.tick(remainingMs = 5_000, enabled = true, playing = true, scrubbing = false).visible)
        assertFalse(AutoNext().tick(remainingMs = 5_000, enabled = false, playing = true, scrubbing = false).visible)
    }
}

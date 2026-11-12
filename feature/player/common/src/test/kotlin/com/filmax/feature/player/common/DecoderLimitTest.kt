package com.filmax.feature.player.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Потолок декодера: понижается на сбое, снимается показанным кадром, Full HD от 2.2.0 не действует. */
class DecoderLimitTest {

    private val saved = mutableListOf<Int?>()
    private val limit = DecoderLimit { saved += it }

    @Test
    fun `failed frame becomes the ceiling and is saved for the device`() {
        limit.lowerTo(failedHeight = UHD, lowerLabel = "1080p")

        assertEquals(UHD, limit.height)
        assertEquals(listOf<Int?>(UHD), saved)
    }

    @Test
    fun `shown frame at the ceiling lifts it, a lower one does not`() {
        limit.restore(UHD)

        limit.liftIfReached(FULL_HD)
        assertEquals(UHD, limit.height)

        limit.liftIfReached(UHD)
        assertNull(limit.height)
        assertEquals(listOf<Int?>(null), saved)
    }

    @Test
    fun `Full HD ceiling left by a busy decoder is ignored`() {
        limit.restore(FULL_HD)

        assertNull(limit.height)
    }

    private companion object {
        const val UHD = 2160
        const val FULL_HD = 1080
    }
}

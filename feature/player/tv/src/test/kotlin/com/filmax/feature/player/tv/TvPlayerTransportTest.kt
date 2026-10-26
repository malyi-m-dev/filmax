package com.filmax.feature.player.tv

import androidx.compose.ui.input.key.Key
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Раскладка пульта в транспорте и подписи времени. */
class TvPlayerTransportTest {

    @Test
    fun `remote keys map to transport commands, foreign keys stay untouched`() {
        assertEquals(TransportCommand.SeekBack, transportCommand(Key.DirectionLeft, nextUpVisible = false))
        assertEquals(TransportCommand.SeekForward, transportCommand(Key.MediaFastForward, nextUpVisible = false))
        assertEquals(TransportCommand.TogglePlay, transportCommand(Key.DirectionCenter, nextUpVisible = false))
        assertEquals(TransportCommand.PlayNext, transportCommand(Key.DirectionCenter, nextUpVisible = true))
        assertEquals(TransportCommand.OpenPanel, transportCommand(Key.DirectionDown, nextUpVisible = false))
        assertEquals(TransportCommand.Peek, transportCommand(Key.DirectionUp, nextUpVisible = false))
        assertNull(transportCommand(Key.VolumeUp, nextUpVisible = false))
    }

    @Test
    fun `remaining time counts down and is empty while duration is unknown`() {
        assertEquals("−21:48", remainingLabel(positionMs = 2_090_000, durationMs = 3_398_000))
        assertEquals("", remainingLabel(positionMs = 0, durationMs = 0))
    }
}

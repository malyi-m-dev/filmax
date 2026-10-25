package com.filmax.data.tvlink

import com.filmax.core.domain.tvlink.TvDevice
import com.filmax.data.tvlink.android.AndroidLinkCrypto
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/** Память связи переживает перезапуск: новый объект на тех же настройках помнит то же. */
class TvLinkMemoryTest {

    private val crypto = AndroidLinkCrypto()
    private val settings = MapSettings()

    @Test
    fun `remembered tv survives a restart until forgotten`() {
        TvLinkMemory(settings, crypto).remember(LIVING_ROOM)

        val restarted = TvLinkMemory(settings, crypto)
        assertEquals(LIVING_ROOM, restarted.paired.value)

        restarted.forget()
        assertNull(TvLinkMemory(settings, crypto).paired.value)
    }

    @Test
    fun `tv keeps its own id across restarts and other tvs get another`() {
        val ownId = TvLinkMemory(settings, crypto).ownId()

        assertEquals(ownId, TvLinkMemory(settings, crypto).ownId())
        assertNotEquals(ownId, TvLinkMemory(MapSettings(), crypto).ownId())
    }

    private companion object {
        val LIVING_ROOM = TvDevice(id = "5f1c9a0e7b3d2a41", name = "Гостиная", host = "192.168.1.50", port = 47100)
    }
}

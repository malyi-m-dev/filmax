package com.filmax.core.domain.user

import com.filmax.core.domain.feedback.ClientInfo
import kotlin.test.Test
import kotlin.test.assertEquals

/** Устройство в списке аккаунта kino.pub узнаётся по модели, типу и версии приложения. */
class DeviceDescriptionTest {

    @Test
    fun `tv is described by its model, as Android TV, with the app version`() {
        val client = ClientInfo(appVersion = "2.3.2", isTv = true, device = "Xiaomi MiTV-MZTU1", os = "Android 14")

        assertEquals(
            DeviceDescription("Xiaomi MiTV-MZTU1", "Android TV", "Filmax 2.3.2 · Android 14"),
            client.deviceDescription(),
        )
    }

    @Test
    fun `phone is described as Android`() {
        val client = ClientInfo(appVersion = "2.3.2", isTv = false, device = "samsung SM-S938B", os = "Android 16")

        assertEquals("Android", client.deviceDescription().hardware)
    }
}

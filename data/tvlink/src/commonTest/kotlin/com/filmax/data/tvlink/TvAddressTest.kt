package com.filmax.data.tvlink

import com.filmax.data.tvlink.protocol.TvLinkProtocol
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Адрес ТВ, введённый на телефоне руками: как его показывает ТВ, так он и должен разбираться. */
class TvAddressTest {

    @Test
    fun `bare address goes to the default port`() {
        val device = assertNotNull(TvAddress.parse(" 192.168.1.50 "))

        assertEquals("192.168.1.50", device.host)
        assertEquals(TvLinkProtocol.DEFAULT_PORT, device.port)
        assertEquals("192.168.1.50", device.name)
    }

    @Test
    fun `explicit port is kept and shown`() {
        val device = assertNotNull(TvAddress.parse("192.168.1.50:47101"))

        assertEquals(47101, device.port)
        assertEquals("192.168.1.50:47101", device.id)
    }

    @Test
    fun `tv shows the address without the default port`() {
        assertEquals("10.0.0.7", TvAddress.format("10.0.0.7", TvLinkProtocol.DEFAULT_PORT))
        assertEquals("10.0.0.7:5000", TvAddress.format("10.0.0.7", 5000))
    }

    @Test
    fun `garbage is not an address`() {
        listOf("", "  ", "192.168.1.50:", "192.168.1.50:99999", "гостиная", "192.168.1.50 47100").forEach {
            assertNull(TvAddress.parse(it), "«$it» принят за адрес")
        }
    }
}

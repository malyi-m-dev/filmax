package com.filmax.data.jellyfin

import com.filmax.data.jellyfin.mapper.IsoDates
import com.filmax.data.jellyfin.mapper.JellyfinMapper
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Мелочи, на которых держится всё остальное: адрес сервера, даты, качество и таблица id. */
class JellyfinBasicsTest {

    @Test
    fun `address without scheme gets http and the default port`() {
        assertEquals("http://192.168.1.20:8096", normalizeAddress(" 192.168.1.20 "))
        assertEquals("http://nas.local:8920", normalizeAddress("nas.local:8920/"))
        assertEquals("http://nas.local:8096/jellyfin", normalizeAddress("nas.local/jellyfin"))
    }

    @Test
    fun `address with scheme is kept as typed`() {
        assertEquals("https://media.example.ru", normalizeAddress("https://media.example.ru/"))
    }

    @Test
    fun `empty address is an error, not a request to nowhere`() {
        assertFailsWith<IllegalArgumentException> { normalizeAddress("  ") }
    }

    @Test
    fun `server dates are read as utc`() {
        assertEquals(1_714_588_212_000, IsoDates.toEpochMillis("2024-05-01T18:30:12.1234567Z"))
        assertEquals(946_684_799_000, IsoDates.toEpochMillis("1999-12-31T23:59:59Z"))
        assertEquals(1_709_164_800_000, IsoDates.toEpochMillis("2024-02-29T00:00:00Z"))
        assertNull(IsoDates.toEpochMillis("вчера"))
    }

    @Test
    fun `quality label follows the frame width for wide movies`() {
        assertEquals("1080p", JellyfinMapper.qualityLabel(width = 1920, height = 800))
        assertEquals("2160p", JellyfinMapper.qualityLabel(width = 3840, height = 1600))
        assertEquals("720p", JellyfinMapper.qualityLabel(width = null, height = 720))
        assertNull(JellyfinMapper.qualityLabel(width = null, height = null))
    }

    @Test
    fun `ids are stable across devices and above kinopub ids`() {
        val phone = JellyfinIds(MapSettings())
        val tv = JellyfinIds(MapSettings())
        val guid = "f27caa37e5142225cceded48f56c4b6e"

        assertEquals(phone.intFor(guid), tv.intFor(guid))
        assertTrue(phone.intFor(guid) >= JellyfinIds.ID_BASE)
        assertEquals(guid, phone.guidFor(phone.intFor(guid)))
    }

    @Test
    fun `colliding guids get different ids`() {
        val ids = JellyfinIds(MapSettings())
        // Одинаковое начало — одинаковое предпочтительное число.
        val first = ids.intFor("abcdef01000000000000000000000001")
        val second = ids.intFor("abcdef01000000000000000000000002")

        assertNotEquals(first, second)
        assertEquals("abcdef01000000000000000000000002", ids.guidFor(second))
    }

    @Test
    fun `ids survive a restart once flushed`() {
        val settings = MapSettings()
        val id = JellyfinIds(settings).run {
            val assigned = intFor("0123456789abcdef0123456789abcdef")
            flush()
            assigned
        }

        assertEquals("0123456789abcdef0123456789abcdef", JellyfinIds(settings).guidFor(id))
    }
}

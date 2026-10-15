package com.filmax.core.domain.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TelemetryTextTest {

    @Test
    fun `secret parameters are masked and the rest stays`() {
        val text = "Socket timeout [url=https://api.host/oauth2/device?grant_type=device_code" +
            "&client_id=android&client_secret=s3cr3t&code=ABC123, socket_timeout=unknown]"

        val redacted = TelemetryText.redact(text)

        assertFalse("s3cr3t" in redacted, redacted)
        assertFalse("ABC123" in redacted, redacted)
        assertTrue("client_secret=***" in redacted && "client_id=android" in redacted, redacted)
    }

    @Test
    fun `request of a status error is its method and path without parameters`() {
        val text = """Client request(GET https://api.host/api/v1/items/new?type=movie&page=1) invalid: 404."""

        assertEquals("GET /api/v1/items/new", TelemetryText.requestOf(text))
    }

    @Test
    fun `request of a timeout is its path`() {
        val text = "Socket timeout has expired [url=https://api.host/api/v1/user, socket_timeout=unknown] ms"

        assertEquals("/api/v1/user", TelemetryText.requestOf(text))
    }

    @Test
    fun `request without parameters keeps the whole path`() {
        val text = "(POST https://h/api/v1/bookmarks/create) 500"

        assertEquals("POST /api/v1/bookmarks/create", TelemetryText.requestOf(text))
    }

    @Test
    fun `text without url has no request`() {
        assertNull(TelemetryText.requestOf("Required value was null."))
    }

    @Test
    fun `long line is cut`() {
        assertTrue(TelemetryText.line("x".repeat(LONG)).length < LONG)
    }

    private companion object {
        const val LONG = 1000
    }
}

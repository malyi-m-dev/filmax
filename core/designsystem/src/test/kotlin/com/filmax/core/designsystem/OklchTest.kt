package com.filmax.core.designsystem

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Перевод oklch макета в sRGB: края шкалы, серый посередине, оттенок по кругу, обрезка вне sRGB. */
class OklchTest {

    @Test
    fun `lightness ends are black and white, chroma zero is gray`() {
        val white = oklch(lightness = 1f, chroma = 0f, hue = 0f)
        val black = oklch(lightness = 0f, chroma = 0f, hue = 0f)
        val gray = oklch(lightness = 0.5f, chroma = 0f, hue = 0f)

        assertEquals(1f, white.red, TOLERANCE)
        assertEquals(1f, white.blue, TOLERANCE)
        assertEquals(0f, black.green, TOLERANCE)
        // L = 0.5 — это 0.125 линейной яркости, в sRGB ≈ 0.39: светлота oklch не линейна, как и глаз.
        assertEquals(MID_GRAY, gray.red, TOLERANCE)
        assertEquals(gray.red, gray.green, TOLERANCE)
        assertEquals(gray.red, gray.blue, TOLERANCE)
    }

    @Test
    fun `hue turns the color around the wheel`() {
        val red = oklch(lightness = 0.6f, chroma = 0.2f, hue = 25f)
        val green = oklch(lightness = 0.6f, chroma = 0.2f, hue = 145f)
        val blue = oklch(lightness = 0.6f, chroma = 0.2f, hue = 265f)

        assertTrue(red.red > red.green && red.red > red.blue, "$red")
        assertTrue(green.green > green.red && green.green > green.blue, "$green")
        assertTrue(blue.blue > blue.red && blue.blue > blue.green, "$blue")
    }

    @Test
    fun `out of gamut color is clipped per channel and alpha is kept`() {
        val vivid = oklch(lightness = 0.9f, chroma = 0.37f, hue = 145f, alpha = 0.4f)

        listOf(vivid.red, vivid.green, vivid.blue).forEach { channel -> assertTrue(channel in 0f..1f) }
        assertEquals(0.4f, vivid.alpha, TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 0.01f
        const val MID_GRAY = 0.389f
    }
}

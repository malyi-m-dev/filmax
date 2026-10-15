package com.filmax.feature.player.common

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlaybackException
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Лестница качеств и распознавание ошибки декодера: слабая приставка, упавшая на 4K, сразу
 * получает 1080p, а в следующий раз стартует с него.
 */
// Robolectric — ради PlaybackException: её конструктор читает SystemClock.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QualityLadderTest {

    private val qualities = listOf("480p", "2160p", "720p", "1080p").map { label ->
        StreamQuality(label, listOf("https://cdn/$label.m3u8"))
    }

    @Test
    fun `height is read from the label`() {
        assertEquals(2160, QualityLadder.heightOf("2160p"))
        assertEquals(2160, QualityLadder.heightOf("4K"))
        assertEquals(720, QualityLadder.heightOf("720p"))
        assertNull(QualityLadder.heightOf("Авто"))
    }

    @Test
    fun `auto without a limit starts with the best quality`() {
        assertEquals("2160p", QualityLadder.start(qualities, AUTO, limit = null)?.label)
    }

    @Test
    fun `limit keeps the start below the height the decoder failed on`() {
        assertEquals("1080p", QualityLadder.start(qualities, AUTO, limit = UHD)?.label)
        assertEquals("1080p", QualityLadder.start(qualities, "2160p", limit = UHD)?.label)
        assertEquals("720p", QualityLadder.start(qualities, "720p", limit = UHD)?.label)
    }

    @Test
    fun `nothing below the limit still starts with something`() {
        assertEquals("2160p", QualityLadder.start(qualities, AUTO, limit = LOWEST)?.label)
    }

    @Test
    fun `step down goes to the best quality below the failed one`() {
        assertEquals("1080p", QualityLadder.below(qualities, UHD)?.label)
        assertNull(QualityLadder.below(qualities, LOWEST))
    }

    @Test
    fun `video decoder failure gives the frame height`() {
        val error = rendererError(MimeTypes.VIDEO_H264, UHD, PlaybackException.ERROR_CODE_DECODING_FAILED)

        assertEquals(UHD, error.videoDecoderFailureHeight())
    }

    @Test
    fun `audio decoder and network failures are not a video decoder failure`() {
        val decodingFailed = PlaybackException.ERROR_CODE_DECODING_FAILED
        val audio = rendererError(MimeTypes.AUDIO_AAC, height = Format.NO_VALUE, decodingFailed)
        val network = PlaybackException("нет сети", null, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED)

        assertNull(audio.videoDecoderFailureHeight())
        assertNull(network.videoDecoderFailureHeight())
    }

    private fun rendererError(mimeType: String, height: Int, errorCode: Int): ExoPlaybackException =
        ExoPlaybackException.createForRenderer(
            IllegalStateException("MediaCodecVideoRenderer error"),
            "MediaCodecVideoRenderer",
            0,
            Format.Builder().setSampleMimeType(mimeType).setHeight(height).build(),
            C.FORMAT_EXCEEDS_CAPABILITIES,
            false,
            errorCode,
        )

    private companion object {
        const val AUTO = "Авто"
        const val UHD = 2160
        const val LOWEST = 480
    }
}

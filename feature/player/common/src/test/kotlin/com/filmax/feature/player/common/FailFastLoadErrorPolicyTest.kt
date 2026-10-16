package com.filmax.feature.player.common

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSpec
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy.LoadErrorInfo
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.SocketTimeoutException
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Пока вариант доставки не заиграл и есть запасной, Media3 не повторяет загрузку: заблокированный
 * CDN иначе держал зрителя ~40 с. Заигравший поток повторяет загрузку как обычно.
 */
// Robolectric — ради Uri в описании загрузки.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FailFastLoadErrorPolicyTest {

    @Test
    fun `variant with a backup that has not started gives up at the first error`() {
        val policy = FailFastLoadErrorPolicy(failFast = { true })

        assertEquals(0, policy.getMinimumLoadableRetryCount(C.DATA_TYPE_MANIFEST))
        assertEquals(0, policy.getMinimumLoadableRetryCount(C.DATA_TYPE_MEDIA))
        // Без этого Media3 запустила бы первый повтор сразу и стёрла ошибку до того, как её увидит плеер.
        assertEquals(C.TIME_UNSET, policy.getRetryDelayMsFor(timeout(errorCount = 1)))
    }

    @Test
    fun `started stream retries as Media3 does by default`() {
        val policy = FailFastLoadErrorPolicy(failFast = { false })

        assertEquals(DEFAULT_RETRIES, policy.getMinimumLoadableRetryCount(C.DATA_TYPE_MANIFEST))
        assertEquals(DEFAULT_RETRIES, policy.getMinimumLoadableRetryCount(C.DATA_TYPE_MEDIA))
        assertEquals(SECOND_RETRY_DELAY_MS, policy.getRetryDelayMsFor(timeout(errorCount = 2)))
    }

    private fun timeout(errorCount: Int) = LoadErrorInfo(
        LoadEventInfo(LoadEventInfo.getNewId(), DataSpec(Uri.parse("https://api.srvkp.com/hls4.m3u8")), 0L),
        MediaLoadData(C.DATA_TYPE_MANIFEST),
        SocketTimeoutException("Read timed out"),
        errorCount,
    )

    private companion object {
        /** DefaultLoadErrorHandlingPolicy.DEFAULT_MIN_LOADABLE_RETRY_COUNT. */
        const val DEFAULT_RETRIES = 3

        /** Дефолтная задержка Media3 перед вторым повтором: (errorCount − 1) секунд. */
        const val SECOND_RETRY_DELAY_MS = 1_000L
    }
}

package com.filmax.feature.player.common

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.filmax.core.domain.common.ErrorReporting
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

/**
 * Варианты доставки одного качества (hls4 → http → hls) и переход между ними. Варианты ведут на
 * разные CDN-хосты, и недоступность одного (SNI-блокировка srvkp.com) не значит, что тайтл не
 * посмотреть.
 *
 * Пока основной вариант с запасным не заиграл, сбой загрузки не повторяется ([failFast], см.
 * [streamSourceFactory]), а молчание обрывает сторож старта ([START_TIMEOUT_MS]). [launch] — область
 * экрана для сторожа, [onSwitched] — какой вариант теперь играет.
 */
internal class StreamFallback(
    private val player: Player,
    private val mediaItemOf: (url: String) -> MediaItem,
    private val launch: (suspend () -> Unit) -> Job,
    private val onSwitched: (url: String) -> Unit,
) {
    /** Читается потоком загрузки Media3. */
    @Volatile
    var failFast = false
        private set

    private var urls: List<String> = emptyList()
    private var index = 0
    private var startWatch: Job? = null

    /** Качество — с основного варианта; позицию и паузу ставит вызывающий. */
    fun start(quality: StreamQuality) {
        urls = quality.urls
        index = 0
        begin(quality.url, primaryWithBackup = urls.size > 1)
    }

    /**
     * Следующий вариант того же качества — с той же позиции и с тем же состоянием паузы: сбой CDN —
     * не повод запускать видео у того, кто стоял на паузе. false — варианты кончились, ошибку
     * показывает вызывающий.
     */
    fun next(): Boolean {
        val nextUrl = urls.getOrNull(index + 1) ?: return false
        index++
        ErrorReporting.reporter.log("player: variant fallback #$index host=${urlHost(nextUrl)}")
        val position = player.currentPosition
        val wasPlaying = player.playWhenReady
        begin(nextUrl, primaryWithBackup = false)
        if (position > 0) player.seekTo(position)
        player.playWhenReady = wasPlaying
        onSwitched(nextUrl)
        return true
    }

    /** Поток заиграл — дальше сбои загрузки повторяются как обычно. */
    fun onReady() {
        failFast = false
    }

    /**
     * Ставит в плеер вариант [url]. Запасные живут на другом CDN и грузятся как обычно, с
     * повторами; основной с запасным — без повторов и под сторожем старта.
     */
    private fun begin(url: String, primaryWithBackup: Boolean) {
        failFast = primaryWithBackup
        player.setMediaItem(mediaItemOf(url))
        player.prepare()
        startWatch?.cancel()
        if (primaryWithBackup) startWatch = launch { watchStart(url) }
    }

    /**
     * Сторож старта: основной вариант, который за [START_TIMEOUT_MS] не отдал ни секунды видео,
     * сменяется запасным, не дожидаясь ошибки, — заблокированный хост молчит до неё полминуты.
     * Медленный, но живой CDN что-то уже отдал, и его не трогаем.
     */
    private suspend fun watchStart(url: String) {
        delay(START_TIMEOUT_MS)
        if (!failFast || player.totalBufferedDuration > 0) return
        ErrorReporting.reporter.log("player: no data in ${START_TIMEOUT_MS}ms host=${urlHost(url)}")
        next()
    }
}

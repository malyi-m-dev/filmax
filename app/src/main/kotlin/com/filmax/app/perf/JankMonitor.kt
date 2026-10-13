package com.filmax.app.perf

import android.util.Log
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.metrics.performance.FrameData
import androidx.metrics.performance.JankStats
import androidx.metrics.performance.PerformanceMetricsState

private const val TAG = "FilmaxJank"

/** Ключ состояния JankStats: на каком экране снят кадр. */
private const val SCREEN_STATE = "screen"

/** Экран до первого перехода навигации (сплэш, онбординг до маршрута). */
private const val UNKNOWN_SCREEN = "unknown"

/** Сводка уходит в лог каждые столько кадров одного экрана — около 10 с при 60 fps. */
private const val REPORT_EVERY_FRAMES = 600

private const val NANOS_IN_MILLI = 1_000_000.0

/**
 * Рывки кадров по экранам в рабочем приложении — то, чего не видно в бенчмарке на эмуляторе:
 * как интерфейс ведёт себя на реальном телевизоре. JankStats помечает каждый кадр текущим
 * экраном ([ReportScreenForJank]), монитор копит по экрану число кадров, долю рывков, среднее и
 * худшее время кадра и пишет сводку в logcat (`adb logcat -s FilmaxJank`): при смене экрана и
 * каждые 600 кадров.
 *
 * Рывок определяет сам JankStats — кадр дольше бюджета частоты экрана с запасом.
 */
class JankMonitor(window: Window) {

    private val screens = mutableMapOf<String, ScreenFrames>()
    private var lastScreen: String? = null
    private val stats = JankStats.createAndTrack(window, ::onFrame)

    /** Слежение только пока активность на экране: в фоне кадров нет, а слушатель стоит денег. */
    fun setTracking(enabled: Boolean) {
        stats.isTrackingEnabled = enabled
        if (!enabled) synchronized(screens) { flushAll() }
    }

    // FrameMetrics на API 24+ отдаёт кадры не в главном потоке — отсюда synchronized.
    private fun onFrame(frame: FrameData) {
        val screen = frame.states.firstOrNull { it.key == SCREEN_STATE }?.value ?: UNKNOWN_SCREEN
        synchronized(screens) {
            if (screen != lastScreen) lastScreen?.let(::flush)
            lastScreen = screen
            val frames = screens.getOrPut(screen) { ScreenFrames() }
            frames.add(frame.frameDurationUiNanos, frame.isJank)
            if (frames.count >= REPORT_EVERY_FRAMES) flush(screen)
        }
    }

    private fun flushAll() {
        screens.keys.toList().forEach(::flush)
    }

    private fun flush(screen: String) {
        val frames = screens.remove(screen) ?: return
        if (frames.count == 0) return
        Log.i(TAG, frames.summary(screen))
    }
}

/** Накопленные кадры одного экрана. */
private class ScreenFrames {
    var count = 0
        private set
    private var janky = 0
    private var totalNanos = 0L
    private var worstNanos = 0L

    fun add(durationNanos: Long, jank: Boolean) {
        count++
        if (jank) janky++
        totalNanos += durationNanos
        worstNanos = maxOf(worstNanos, durationNanos)
    }

    fun summary(screen: String): String {
        val jankPercent = janky * PERCENT / count
        val average = totalNanos / count / NANOS_IN_MILLI
        val worst = worstNanos / NANOS_IN_MILLI
        return "%s: кадров %d, рывков %d (%.1f%%), в среднем %.1f мс, худший %.1f мс"
            .format(screen, count, janky, jankPercent, average, worst)
    }

    private companion object {
        const val PERCENT = 100.0
    }
}

/**
 * Помечает кадры экраном [route] — по нему монитор группирует рывки. Ставится там, где известен
 * текущий маршрут навигации; полное имя класса маршрута сокращается до простого.
 */
@Composable
fun ReportScreenForJank(route: String?) {
    val view = LocalView.current
    val screen = route?.let(::screenName) ?: UNKNOWN_SCREEN
    DisposableEffect(view, screen) {
        val holder = PerformanceMetricsState.getHolderForHierarchy(view)
        holder.state?.putState(SCREEN_STATE, screen)
        onDispose { holder.state?.removeState(SCREEN_STATE) }
    }
}

/** «com.filmax.app.tv.navigation.TvHomeRoute» → «TvHomeRoute», аргументы маршрута отрезаются. */
internal fun screenName(route: String): String =
    route.substringBefore('/').substringBefore('?').substringBefore('{').substringAfterLast('.')

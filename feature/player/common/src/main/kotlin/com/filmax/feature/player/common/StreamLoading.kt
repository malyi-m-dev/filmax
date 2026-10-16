package com.filmax.feature.player.common

import android.content.Context
import androidx.media3.common.C
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy

/**
 * Как плеер грузит поток. Варианты доставки одного качества лежат на разных CDN, и недоступный
 * основной (SNI-блокировка api.srvkp.com) держал зрителя у чёрного экрана ~40 с: Android перебирает
 * все адреса хоста с полным таймаутом на каждый, Media3 повторяет загрузку, и только потом плеер
 * переходил на запасной вариант.
 *
 * Пока основной вариант не заиграл и у него есть запасной ([failFast]), ошибку загрузки не
 * повторяем: она сразу доходит до плеера, и тот переключается. Если хост просто молчит, вариант
 * сменяет сторож старта (см. [START_TIMEOUT_MS]). Запасные варианты и заигравший поток повторяют
 * загрузку как обычно: сбой на живом CDN скорее временный, и менять из-за него поток незачем.
 */
internal fun streamSourceFactory(context: Context, failFast: () -> Boolean): MediaSource.Factory =
    DefaultMediaSourceFactory(context).setLoadErrorHandlingPolicy(FailFastLoadErrorPolicy(failFast))

/**
 * Повторы загрузки Media3, которые выключаются, пока [failFast] (см. [streamSourceFactory]). Мало
 * обнулить минимум повторов: первый повтор Media3 запускает без задержки и тем стирает ошибку
 * раньше, чем плеер её увидит. Поэтому ошибка сразу объявляется окончательной.
 */
internal class FailFastLoadErrorPolicy(private val failFast: () -> Boolean) : DefaultLoadErrorHandlingPolicy() {

    override fun getMinimumLoadableRetryCount(dataType: Int): Int =
        if (failFast()) 0 else super.getMinimumLoadableRetryCount(dataType)

    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long =
        if (failFast()) C.TIME_UNSET else super.getRetryDelayMsFor(loadErrorInfo)
}

/**
 * Сколько основной вариант с запасным может молчать на старте. Ошибки заблокированного хоста ждать
 * долго: Android перебирает все его адреса (у api.srvkp.com их четыре) с таймаутом 8 с на каждый.
 * Рабочий CDN за это время успевает отдать первые секунды видео.
 */
internal const val START_TIMEOUT_MS = 8_000L

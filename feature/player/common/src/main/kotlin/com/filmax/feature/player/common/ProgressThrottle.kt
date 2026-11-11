package com.filmax.feature.player.common

import kotlin.math.abs

/**
 * Троттлинг прогресса по позиции: пока не отъехали от последней отправки дальше [stepSeconds],
 * сервер не дёргаем — тик плеера идёт раз в секунду, а это на порядок чаще, чем нужно. Перемотка
 * назад — тоже отъезд: позиция уходит на сервер сразу.
 */
internal class ProgressThrottle(private val stepSeconds: Int = PROGRESS_STEP_SECONDS) {

    private var lastSent: Int? = null

    /** true — позицию [seconds] пора отправить; она же становится новой точкой отсчёта. */
    fun shouldSend(seconds: Int): Boolean {
        val sent = lastSent
        if (sent != null && abs(seconds - sent) < stepSeconds) return false
        lastSent = seconds
        return true
    }

    private companion object {
        /** Реже, чем тик плеера (1 с), но чаще, чем теряется место. */
        const val PROGRESS_STEP_SECONDS = 5
    }
}

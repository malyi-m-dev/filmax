package com.filmax.app

import android.util.Log
import com.filmax.core.domain.common.ErrorReporter

/**
 * Debug-репортер: те же события телеметрии, но в logcat (тег [TAG]) — Crashlytics в debug
 * не собирает, а без локального вывода хлебные крошки плеера и non-fatal'ы невозможно
 * проверить на эмуляторе.
 */
internal class LogcatErrorReporter : ErrorReporter {

    override fun setUser(id: String?) {
        Log.i(TAG, "user: ${id ?: "-"}")
    }

    override fun log(message: String) {
        Log.i(TAG, message)
    }

    override fun report(error: Throwable) {
        Log.w(TAG, "non-fatal", error)
    }

    private companion object {
        const val TAG = "FilmaxTelemetry"
    }
}

package com.filmax.app

import com.filmax.core.domain.common.ErrorReporter
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * [ErrorReporter] поверх Crashlytics: non-fatal ошибки data-слоя (HTTP-статусы, падения
 * парсинга), хлебные крошки и привязка отчётов к пользователю. Версию приложения, модель
 * устройства и ОС Crashlytics прикладывает к каждому отчёту сам.
 */
internal class CrashlyticsErrorReporter(
    private val crashlytics: FirebaseCrashlytics,
) : ErrorReporter {

    override fun setUser(id: String?) = crashlytics.setUserId(id.orEmpty())

    override fun log(message: String) = crashlytics.log(message)

    override fun report(error: Throwable) = crashlytics.recordException(error)
}

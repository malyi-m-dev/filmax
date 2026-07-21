package com.filmax.core.domain.common

import kotlin.concurrent.Volatile

/**
 * Приёмник телеметрии ошибок (на Android — Crashlytics). Контракт живёт в domain, чтобы граница
 * ошибок [safeRequest] и репозитории могли репортить без зависимости от платформы: конкретную
 * реализацию подставляет app-модуль на старте, до этого момента работает no-op.
 */
interface ErrorReporter {

    /** Привязывает последующие отчёты к пользователю (username kino.pub); null — сброс при logout. */
    fun setUser(id: String?)

    /** Хлебная крошка: строка попадает в хронологию ближайшего отчёта, сама по себе не событие. */
    fun log(message: String)

    /** Non-fatal событие: ошибка запроса/парсинга — всё, что пережил [safeRequest]. */
    fun report(error: Throwable)
}

/**
 * Глобальная точка доступа к репортеру. Держатель, а не Koin: [safeRequest] — inline-функция
 * верхнего уровня без DI, и тащить инжекцию во все репозитории ради телеметрии незачем.
 */
object ErrorReporting {

    @Volatile
    var reporter: ErrorReporter = NoopErrorReporter

    private object NoopErrorReporter : ErrorReporter {
        override fun setUser(id: String?) = Unit
        override fun log(message: String) = Unit
        override fun report(error: Throwable) = Unit
    }
}

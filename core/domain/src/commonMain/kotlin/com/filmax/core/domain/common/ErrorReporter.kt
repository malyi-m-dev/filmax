package com.filmax.core.domain.common

import com.filmax.core.domain.error.AppError
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

/**
 * Отправляет сбой запроса с оглядкой на его класс: событием уходит только то, что похоже на
 * баг (5xx, падение парсинга), а ожидаемое в эксплуатации — нет сети, таймаут, протухшая
 * сессия, нет подписки — остаётся хлебной крошкой.
 *
 * Без этого деления один вход в приложение из метро давал пять non-fatal (главная тянет пять
 * запросов параллельно), и настоящие баги тонули бы в отчётах о плохом канале связи.
 */
fun ErrorReporter.reportRequestFailure(error: Throwable) {
    val kind = AppError.resolve(error.message, error)
    if (kind in REPORTED_ERRORS) {
        report(error)
    } else {
        log("request failed (${kind.name}): ${error::class.simpleName}: ${error.message}")
    }
}

/** Классы сбоев, которые едут в телеметрию событием: остальные — крошкой (см. [reportRequestFailure]). */
private val REPORTED_ERRORS = setOf(AppError.Server, AppError.Empty, AppError.Playback)

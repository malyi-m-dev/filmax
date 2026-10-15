package com.filmax.core.domain.common

import com.filmax.core.domain.error.AppError
import com.filmax.core.domain.error.RequestFailure
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

    /**
     * Ключ, который приложится к каждому следующему отчёту: на каком экране был пользователь, какой
     * запрос упал. В отличие от крошки, ключ — фильтр в консоли Crashlytics.
     */
    fun setKey(key: String, value: String)

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
        override fun setKey(key: String, value: String) = Unit
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
 *
 * Событие уходит обёрнутым в [RequestFailure]: тип non-fatal Crashlytics пишет строкой из
 * рантайма и не деобфусцирует — с обёрткой заголовок issue читается («Server», «Empty»),
 * а стек первопричины остаётся в cause. Какой запрос упал, говорит ключ [REQUEST_KEY]: все
 * сбои одного класса Crashlytics складывает в один issue, и без ключа их не различить.
 *
 * Секретные параметры URL в текст не попадают ([TelemetryText.redact]).
 */
fun ErrorReporter.reportRequestFailure(error: Throwable) {
    val kind = AppError.resolve(error.message, error)
    val text = TelemetryText.redact("${error::class.simpleName}: ${error.message}")
    if (kind !in REPORTED_ERRORS) {
        log(TelemetryText.line("request failed (${kind.name}): $text"))
        return
    }
    setKey(REQUEST_KEY, TelemetryText.requestOf(error.message.orEmpty()) ?: "-")
    log(TelemetryText.line("request error (${kind.name}): $text"))
    report(RequestFailure.of(kind, error))
}

/** Ключ отчёта: метод и путь запроса, который упал последним событием. */
const val REQUEST_KEY = "request"

/** Классы сбоев, которые едут в телеметрию событием: остальные — крошкой (см. [reportRequestFailure]). */
private val REPORTED_ERRORS = setOf(AppError.Server, AppError.Empty, AppError.Playback)

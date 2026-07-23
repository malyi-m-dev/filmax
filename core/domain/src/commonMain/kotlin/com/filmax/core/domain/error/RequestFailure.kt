package com.filmax.core.domain.error

/**
 * Типизированная обёртка сбоя data-слоя для телеметрии (см. `reportRequestFailure`).
 *
 * Свой подкласс на каждый [AppError] сделан намеренно: тип non-fatal исключения Crashlytics
 * записывает строкой из рантайма и mapping его НЕ деобфусцирует — в списке issues были «n8.a»
 * и «S6.k.p» вместо понятных типов. С обёрткой заголовок читается сразу («RequestFailure$Server»),
 * а первопричина остаётся в [cause] — её стек в отчёте деобфусцируется загруженным mapping.
 */
sealed class RequestFailure(message: String, cause: Throwable) : Exception(message, cause) {
    class Offline(message: String, cause: Throwable) : RequestFailure(message, cause)
    class Server(message: String, cause: Throwable) : RequestFailure(message, cause)
    class Timeout(message: String, cause: Throwable) : RequestFailure(message, cause)
    class NotFound(message: String, cause: Throwable) : RequestFailure(message, cause)
    class Empty(message: String, cause: Throwable) : RequestFailure(message, cause)
    class Premium(message: String, cause: Throwable) : RequestFailure(message, cause)
    class Region(message: String, cause: Throwable) : RequestFailure(message, cause)
    class Auth(message: String, cause: Throwable) : RequestFailure(message, cause)
    class Playback(message: String, cause: Throwable) : RequestFailure(message, cause)

    companion object {
        fun of(kind: AppError, cause: Throwable): RequestFailure {
            // Сообщения Ktor тащат полный URL с параметрами — в заголовке события хватает начала.
            val text = "${cause::class.simpleName}: ${cause.message?.take(MAX_CAUSE_LENGTH)}"
            return when (kind) {
                AppError.Offline -> Offline(text, cause)
                AppError.Server -> Server(text, cause)
                AppError.Timeout -> Timeout(text, cause)
                AppError.NotFound -> NotFound(text, cause)
                AppError.Empty -> Empty(text, cause)
                AppError.Premium -> Premium(text, cause)
                AppError.Region -> Region(text, cause)
                AppError.Auth -> Auth(text, cause)
                AppError.Playback -> Playback(text, cause)
            }
        }

        private const val MAX_CAUSE_LENGTH = 300
    }
}

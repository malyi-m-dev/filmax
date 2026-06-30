package com.filmax.core.domain.error

import com.filmax.core.domain.common.RequestResult

/**
 * Семантический тип ошибки приложения. Презентационный слой (ScreenModel) резолвит
 * «сырую» ошибку запроса в [AppError] через [AppError.resolve] и показывает нужную
 * модалку. UI-слой маппит [AppError] на иконку/цвет/тексты.
 */
enum class AppError {
    /** Нет сети / не удалось установить соединение. */
    Offline,

    /** Сбой на стороне сервера (5xx). */
    Server,

    /** Сервер не ответил вовремя (408 / timeout). */
    Timeout,

    /** Контент не найден (404). */
    NotFound,

    /** Пустой результат (запрос успешен, но данных нет). */
    Empty,

    /** Требуется подписка Premium (402). */
    Premium,

    /** Контент недоступен в регионе (403). */
    Region,

    /** Сессия истекла / не авторизован (401). */
    Auth,

    /** Ошибка воспроизведения видео. */
    Playback,
    ;

    companion object {
        /**
         * Резолвит ошибку запроса в [AppError] по сообщению и причине.
         * Работает в commonMain — без зависимостей от Ktor/Android, по эвристике
         * (имя класса исключения + текст с HTTP-кодом).
         */
        fun resolve(message: String?, cause: Throwable? = null): AppError {
            val causeName = cause?.let { it::class.simpleName.orEmpty() }.orEmpty()
            val text = buildString {
                append(message.orEmpty())
                append(' ')
                append(cause?.message.orEmpty())
            }

            return when {
                isOffline(causeName, text) -> Offline
                isTimeout(causeName, text) -> Timeout
                hasStatus(text, HTTP_UNAUTHORIZED) || text.contains("unauthorized", ignoreCase = true) -> Auth
                hasStatus(text, HTTP_PAYMENT_REQUIRED) -> Premium
                hasStatus(text, HTTP_FORBIDDEN) || text.contains("forbidden", ignoreCase = true) -> Region
                hasStatus(text, HTTP_NOT_FOUND) || text.contains("not found", ignoreCase = true) -> NotFound
                else -> Server
            }
        }

        /** Нет сети / не удалось установить соединение (по имени исключения или тексту). */
        private fun isOffline(causeName: String, text: String): Boolean =
            causeName.contains("UnknownHost") ||
                causeName.contains("ConnectException") ||
                causeName.contains("UnresolvedAddress") ||
                text.contains("Unable to resolve host", ignoreCase = true) ||
                text.contains("Failed to connect", ignoreCase = true)

        /** Сервер не ответил вовремя (timeout / 408). */
        private fun isTimeout(causeName: String, text: String): Boolean =
            causeName.contains("Timeout") ||
                text.contains("timeout", ignoreCase = true) ||
                hasStatus(text, HTTP_REQUEST_TIMEOUT)

        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_PAYMENT_REQUIRED = 402
        private const val HTTP_FORBIDDEN = 403
        private const val HTTP_NOT_FOUND = 404
        private const val HTTP_REQUEST_TIMEOUT = 408

        private fun hasStatus(text: String, code: Int): Boolean =
            Regex("""\b$code\b""").containsMatchIn(text)
    }
}

/** Резолвит [RequestResult.Error] в семантический [AppError]. */
fun RequestResult.Error.toAppError(): AppError = AppError.resolve(message, cause)

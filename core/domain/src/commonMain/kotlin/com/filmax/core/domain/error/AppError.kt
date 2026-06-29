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

            if (
                causeName.contains("UnknownHost") ||
                causeName.contains("ConnectException") ||
                causeName.contains("UnresolvedAddress") ||
                text.contains("Unable to resolve host", ignoreCase = true) ||
                text.contains("Failed to connect", ignoreCase = true)
            ) {
                return Offline
            }

            if (
                causeName.contains("Timeout") ||
                text.contains("timeout", ignoreCase = true) ||
                hasStatus(text, 408)
            ) {
                return Timeout
            }

            return when {
                hasStatus(text, 401) || text.contains("unauthorized", ignoreCase = true) -> Auth
                hasStatus(text, 402) -> Premium
                hasStatus(text, 403) || text.contains("forbidden", ignoreCase = true) -> Region
                hasStatus(text, 404) || text.contains("not found", ignoreCase = true) -> NotFound
                STATUS_5XX.containsMatchIn(text) -> Server
                else -> Server
            }
        }

        private val STATUS_5XX = Regex("""\b5\d\d\b""")

        private fun hasStatus(text: String, code: Int): Boolean =
            Regex("""\b$code\b""").containsMatchIn(text)
    }
}

/** Резолвит [RequestResult.Error] в семантический [AppError]. */
fun RequestResult.Error.toAppError(): AppError = AppError.resolve(message, cause)

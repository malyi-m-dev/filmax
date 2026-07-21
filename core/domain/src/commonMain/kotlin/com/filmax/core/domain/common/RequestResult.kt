package com.filmax.core.domain.common

import kotlinx.coroutines.CancellationException

/**
 * Унифицированный результат запроса к данным.
 * Репозиторий ловит ошибку через [safeRequest] и отдаёт в presentation уже готовый результат —
 * никаких try/catch в ViewModel.
 */
sealed interface RequestResult<out T> {
    data class Success<out T>(val data: T) : RequestResult<T>
    data class Error(val message: String?, val cause: Throwable? = null) : RequestResult<Nothing>
}

/** Выполняет [block], оборачивая исключения в [RequestResult.Error]. CancellationException пробрасывается. */
// Намеренная граница ошибок: любой сбой запроса конвертируется в RequestResult.Error (кроме отмены).
@Suppress("TooGenericExceptionCaught")
suspend inline fun <T> safeRequest(crossinline block: suspend () -> T): RequestResult<T> =
    try {
        RequestResult.Success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        // Единственная точка, где видны ВСЕ сбои data-слоя: HTTP-статусы (expectSuccess=true даёт
        // исключение с URL и кодом, включая 500-е) и падения парсинга. Уходит в телеметрию non-fatal.
        ErrorReporting.reporter.report(error)
        RequestResult.Error(error.message, error)
    }

inline fun <T, R> RequestResult<T>.map(transform: (T) -> R): RequestResult<R> = when (this) {
    is RequestResult.Success -> RequestResult.Success(transform(data))
    is RequestResult.Error -> this
}

inline fun <T> RequestResult<T>.onSuccess(block: (T) -> Unit): RequestResult<T> {
    if (this is RequestResult.Success) block(data)
    return this
}

inline fun <T> RequestResult<T>.onError(block: (String?) -> Unit): RequestResult<T> {
    if (this is RequestResult.Error) block(message)
    return this
}

fun <T> RequestResult<T>.getOrNull(): T? = (this as? RequestResult.Success)?.data

/** Первое сообщение об ошибке среди результатов, либо null если все успешны. */
fun firstErrorMessage(vararg results: RequestResult<*>): String? =
    results.firstNotNullOfOrNull { (it as? RequestResult.Error)?.message }

package com.filmax.data.fake

import com.filmax.core.domain.common.RequestResult

/** Ответ «сервера»: сначала задержка сети, затем результат — как у настоящих репозиториев. */
internal suspend fun <T> FakeBackend.respond(block: FakeBackend.() -> RequestResult<T>): RequestResult<T> {
    latency.await()
    return block()
}

internal fun <T> success(value: T): RequestResult<T> = RequestResult.Success(value)

/**
 * 404 тем же текстом, каким его пишет Ktor: `AppError.resolve` находит код в сообщении, и экран
 * покажет «не найдено», как на живом API.
 */
internal fun notFound(what: String): RequestResult.Error = RequestResult.Error("HTTP 404 Not Found: $what")

/**
 * Ответ OAuth-эндпоинта, пока код не подтверждён. Для онбординга это не сбой, а «опрашивай
 * дальше» — так же, как 400 authorization_pending у kino.pub.
 */
internal fun authorizationPending(): RequestResult.Error =
    RequestResult.Error("HTTP 400 Bad Request: authorization_pending")

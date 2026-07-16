package com.filmax.core.network

import io.ktor.client.plugins.logging.Logger

/**
 * Логгер HTTP, вычищающий секреты из строк перед записью.
 *
 * `sanitizeHeader` у Ktor режет только заголовки, а OAuth-обмен kino.pub передаёт секреты
 * **в query-параметрах**: `oauth2/device?grant_type=refresh_token&refresh_token=…`. Без маскировки
 * refresh-токен уходил в logcat целиком — а он даёт бессрочный доступ к аккаунту (по нему
 * выпускается новый access, пока пользователь не разлогинится). Logcat читает любое приложение
 * с отладкой и любой, у кого есть adb, так что это полноценная утечка, а не «просто отладка».
 *
 * Маскируем и [MASKED_PARAMS], и тела ответов OAuth: токены приходят обратно в JSON.
 */
internal class SecretMaskingLogger(private val delegate: Logger) : Logger {

    override fun log(message: String) {
        delegate.log(mask(message))
    }

    private fun mask(message: String): String =
        MASKED_PARAMS.fold(message) { masked, param -> param.regex.replace(masked, param.replacement) }

    private companion object {
        /**
         * Что прячем. `client_secret` тоже маскируем: он зашит в APK и секретом де-факто не
         * является, но светить его в логах — приглашение скопировать.
         */
        val MASKED_PARAMS = listOf(
            // Query-параметры: ?refresh_token=xxx&... → ?refresh_token=***&...
            MaskRule("""(?<=[?&](refresh_token|access_token|client_secret|code)=)[^&\s]+""", "***"),
            // JSON-тела OAuth: "access_token":"xxx" → "access_token":"***"
            MaskRule("""(?<="(refresh_token|access_token)":")[^"]+""", "***"),
        )
    }
}

/** Правило маскировки: что найти и чем заменить. */
private class MaskRule(pattern: String, val replacement: String) {
    val regex = Regex(pattern)
}

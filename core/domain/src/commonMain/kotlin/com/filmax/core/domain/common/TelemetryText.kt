package com.filmax.core.domain.common

/**
 * Текст для телеметрии: что из сообщений об ошибках можно отправить и как его прочитать.
 *
 * Сообщения Ktor несут полный URL с параметрами, а в запросе входа там `client_secret` и коды
 * устройства — в логах Crashlytics они лежали открытым текстом.
 */
object TelemetryText {

    /** Значения секретных параметров заменены на `***`; остальной текст — как был. */
    fun redact(text: String): String = SECRET_PARAM.replace(text) { "${it.groupValues[1]}=***" }

    /**
     * Метод и путь запроса без параметров — «GET /api/v1/items/new». По нему в Crashlytics видно,
     * какой запрос упал, а событие с параметрами (id, страницы) не дробится на тысячи разных.
     * Null — в тексте нет URL.
     */
    fun requestOf(text: String): String? {
        val match = REQUEST.find(text) ?: return null
        val method = match.groupValues[1].ifEmpty { null }
        val path = match.groupValues[2].ifEmpty { "/" }
        return listOfNotNull(method, path).joinToString(" ")
    }

    /** Строка крошки не длиннее [MAX_LINE]: лог Crashlytics ограничен 64 КБ на сессию. */
    fun line(text: String): String = redact(text).let { if (it.length > MAX_LINE) it.take(MAX_LINE) + "…" else it }

    private val SECRET_PARAM = Regex(
        """\b(client_secret|code|device_code|access_token|refresh_token|password)=[^&\s\]),"]+""",
        RegexOption.IGNORE_CASE,
    )

    /** «GET https://host/path?q» у Ktor-ошибок статуса и «url=https://host/path» у таймаутов. */
    private val REQUEST = Regex("""(?:\b(GET|POST|PUT|PATCH|DELETE|HEAD)\s+|url=)https?://[^/\s?]+([^?\s,\])]*)""")

    private const val MAX_LINE = 300
}

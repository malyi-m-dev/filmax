package com.filmax.data.tvlink.protocol

/**
 * Константы протокола «телефон ↔ ТВ». Протокол — строки JSON по TCP, по сообщению на строку: его
 * легко читать в логах и повторить на iOS без общих бинарных форматов.
 */
internal object TvLinkProtocol {

    /** Версия протокола: ТВ объявляет её в mDNS и в приветствии. */
    const val VERSION = 1

    /** Порт приёмника. Постоянный — чтобы адрес, введённый вручную, не менялся от запуска к запуску. */
    const val DEFAULT_PORT = 47100

    /** Тип сервиса DNS-SD, под которым ТВ виден в сети. */
    const val SERVICE_TYPE = "_filmax._tcp"

    /** TXT-атрибуты сервиса: отпечаток аккаунта, версия протокола и постоянный id ТВ. */
    const val ATTR_FINGERPRINT = "acc"
    const val ATTR_VERSION = "v"
    const val ATTR_ID = "id"

    /** Как часто стороны напоминают о себе, когда сказать нечего. */
    const val HEARTBEAT_MS = 3_000L

    /** Молчание дольше этого — связь потеряна (Wi-Fi пропал, ТВ выключили из розетки). */
    const val IDLE_TIMEOUT_MS = 10_000L

    const val CONNECT_TIMEOUT_MS = 5_000L
    const val HANDSHAKE_TIMEOUT_MS = 5_000L

    /** Предел длины строки: чужой клиент в сети не заставит ТВ копить бесконечную строку в памяти. */
    const val MAX_LINE_LENGTH = 16 * 1024
}

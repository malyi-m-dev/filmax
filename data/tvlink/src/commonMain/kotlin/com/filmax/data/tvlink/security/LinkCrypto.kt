package com.filmax.data.tvlink.security

/**
 * Криптография связи — платформенная (на Android javax.crypto и SecureRandom): своя реализация
 * хэшей в общем коде была бы и лишним риском, и лишней зависимостью.
 */
internal interface LinkCrypto {

    fun sha256(data: ByteArray): ByteArray

    fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray

    /** Криптостойкие случайные байты — для чисел рукопожатия. */
    fun randomBytes(size: Int): ByteArray
}

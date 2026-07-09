package com.filmax.core.domain.common

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Лёгкий in-memory кэш последнего успешного значения — основа офлайн-устойчивости (issue #42).
 * Регистрируется как `single` в DI, поэтому переживает пересоздание use-case/ScreenModel и
 * позволяет отдать ранее загруженный контент, когда сеть недоступна.
 *
 * Хранение только в памяти (сбрасывается при перезапуске процесса) — этого достаточно для
 * graceful degradation в рамках сессии; при необходимости персистентность добавляется отдельно.
 */
class LastValueCache<T : Any> {
    private val mutex = Mutex()
    private var cached: T? = null

    suspend fun get(): T? = mutex.withLock { cached }

    suspend fun put(value: T) = mutex.withLock { cached = value }
}

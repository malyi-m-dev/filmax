package com.filmax.data.source

import com.filmax.core.domain.source.ContentSource
import com.filmax.core.domain.source.ContentSourceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Реализация контракта [T] для выбранного источника. Обе реализации ленивые: невыбранный
 * источник не создаётся вовсе, и, скажем, «Буду смотреть» kino.pub не идёт в сеть за своей
 * папкой, пока человек смотрит домашний сервер.
 *
 * Запрос читает источник в момент вызова ([current]), поток — подписывается на смену источника
 * ([flow]): подписчик потока переживает переключение и получает значения уже нового источника.
 */
internal class SourceSwitch<T>(
    private val sources: ContentSourceRepository,
    private val kinopub: Lazy<T>,
    private val homeServer: Lazy<T>,
) {
    val current: T get() = of(sources.source.value)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun <R> flow(select: (T) -> Flow<R>): Flow<R> = sources.source.flatMapLatest { select(of(it)) }

    private fun of(source: ContentSource): T = when (source) {
        ContentSource.KINOPUB -> kinopub.value
        ContentSource.JELLYFIN -> homeServer.value
    }
}

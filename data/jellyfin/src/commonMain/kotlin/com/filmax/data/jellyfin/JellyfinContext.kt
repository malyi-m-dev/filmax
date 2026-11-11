package com.filmax.data.jellyfin

import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.common.safeRequest
import com.filmax.data.jellyfin.mapper.JellyfinImages
import com.filmax.data.jellyfin.mapper.JellyfinMapper
import com.filmax.data.jellyfin.remote.JellyfinApi

/** Общее для репозиториев сервера: API, сессия, таблица id и запрос с разбором ответа. */
internal class JellyfinContext(
    val api: JellyfinApi,
    val ids: JellyfinIds,
    val session: JellyfinSession,
) {

    /**
     * Запрос к серверу: сбой — в [RequestResult.Error], как у kino.pub, а id, выданные при разборе
     * ответа, — на диск одной записью. Маппер собирается под адрес текущей сессии: картинки
     * лежат на том же сервере.
     */
    suspend fun <T> request(block: suspend (JellyfinMapper) -> T): RequestResult<T> = safeRequest {
        try {
            block(JellyfinMapper(ids, JellyfinImages(session.require().address)))
        } finally {
            ids.flush()
        }
    }
}

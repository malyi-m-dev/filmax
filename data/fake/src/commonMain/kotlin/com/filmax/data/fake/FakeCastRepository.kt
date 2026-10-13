package com.filmax.data.fake

import com.filmax.core.domain.person.CastMember
import com.filmax.core.domain.person.CastRepository

/**
 * Актёры с фото вместо TMDB. Ведёт себя как настоящий: ищет тайтл по IMDb-id и молча отдаёт
 * пустой список, если id нет или он незнаком, — тогда детали показывают инициалы. Фото
 * «доезжают» с задержкой сети, уже после показа тайтла, как и с живым TMDB.
 */
class FakeCastRepository(private val backend: FakeBackend) : CastRepository {

    override suspend fun getCast(imdbId: String?): List<CastMember> {
        val id = imdbId?.takeIf { it.isNotBlank() } ?: return emptyList()
        backend.latency.await()
        return backend.catalog.entryByImdbId(id)?.cast.orEmpty()
    }
}

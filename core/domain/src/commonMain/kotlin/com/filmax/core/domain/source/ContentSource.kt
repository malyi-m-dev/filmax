package com.filmax.core.domain.source

import kotlinx.coroutines.flow.StateFlow

/**
 * Откуда приложение берёт каталог и видео. kino.pub — сервис по подписке; домашний сервер
 * (Jellyfin) — своя медиатека в локальной сети: фильмы и сериалы, которые лежат у человека дома.
 *
 * Источник один на всё приложение. Экраны о нём не знают: под ними целиком переключаются
 * репозитории (data:source), а граф по смене источника возвращается к корню раздела — иначе на
 * экране остались бы тайтлы прежнего источника с его id.
 */
enum class ContentSource {
    KINOPUB,
    JELLYFIN,
}

/** Выбранный источник. Хранится на устройстве: у телефона и ТВ он может быть разным. */
interface ContentSourceRepository {
    val source: StateFlow<ContentSource>

    suspend fun select(source: ContentSource)
}

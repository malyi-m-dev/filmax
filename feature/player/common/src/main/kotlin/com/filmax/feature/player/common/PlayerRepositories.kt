package com.filmax.feature.player.common

import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.playback.StreamRepository
import com.filmax.core.domain.source.ContentSourceRepository
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.watching.WatchingRepository

/**
 * Репозитории, с которыми работает плеер: тайтл и его серии, потоки под запуск, прогресс
 * просмотра, подписка аккаунта и источник (нужна ли подписка вообще). Одним объектом — иначе
 * конструктор [PlayerScreenModel] за порогом detekt.
 */
class PlayerRepositories(
    val catalog: CatalogRepository,
    val watching: WatchingRepository,
    val user: UserRepository,
    val streams: StreamRepository,
    val sources: ContentSourceRepository,
)

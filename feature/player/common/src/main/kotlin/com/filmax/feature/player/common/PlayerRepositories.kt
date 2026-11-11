package com.filmax.feature.player.common

import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.playback.StreamRepository
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.watching.WatchingRepository

/**
 * Репозитории, с которыми работает плеер: тайтл и его серии, потоки под запуск, прогресс
 * просмотра и подписка аккаунта. Одним объектом — иначе конструктор [PlayerScreenModel] за
 * порогом detekt.
 */
class PlayerRepositories(
    val catalog: CatalogRepository,
    val watching: WatchingRepository,
    val user: UserRepository,
    val streams: StreamRepository,
)

package com.filmax.feature.player.common

import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.watching.WatchingRepository

/**
 * Репозитории kino.pub, с которыми работает плеер: тайтл и его серии, прогресс просмотра и
 * подписка аккаунта. Одним объектом — иначе конструктор [PlayerScreenModel] за порогом detekt.
 */
class PlayerRepositories(
    val catalog: CatalogRepository,
    val watching: WatchingRepository,
    val user: UserRepository,
)

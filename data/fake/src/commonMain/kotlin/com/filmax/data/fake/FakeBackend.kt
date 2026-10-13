package com.filmax.data.fake

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemPage
import com.filmax.data.fake.fixtures.buildFixtureCatalog
import com.filmax.data.fake.fixtures.initialFolders
import com.filmax.data.fake.fixtures.initialWatchSnapshot
import com.filmax.data.fake.store.AccountStore
import com.filmax.data.fake.store.AuthStore
import com.filmax.data.fake.store.CatalogEntry
import com.filmax.data.fake.store.FakeCatalog
import com.filmax.data.fake.store.LibraryStore
import com.filmax.data.fake.store.WatchStore
import com.filmax.data.fake.store.pageOf
import com.filmax.data.fake.store.paginationOf

/**
 * «Сервер» мок-режима: неизменяемый каталог-фикстура плюс состояние пользователя на одну сессию —
 * вход, закладки, флаг watchlist, прогресс просмотра, настройки устройства, уведомления.
 *
 * Репозитории — тонкие фасады над ним, а состояние общее: отметка из плеера сразу видна в деталях
 * и в «Продолжить», «Буду смотреть» из деталей — в «Моём». Между запусками ничего не хранится:
 * каждый старт мок-сборки начинается с одной и той же картины.
 *
 * Один экземпляр — один «аккаунт». В мок-сборке это Koin-синглтон; тест заводит свой (обычно с
 * [FakeLatency.None]) и получает чистое состояние. [authenticated] = false — старт с онбординга.
 */
class FakeBackend(
    internal val latency: FakeLatency = FakeLatency.Realistic,
    clock: () -> Long = ::currentEpochMillis,
    authenticated: Boolean = true,
) {
    internal val catalog: FakeCatalog = buildFixtureCatalog()
    internal val library: LibraryStore = LibraryStore(initialFolders(now = clock()), clock)
    internal val watch: WatchStore = WatchStore(initialWatchSnapshot(catalog))
    internal val auth: AuthStore = AuthStore(authenticated)
    internal val account: AccountStore = AccountStore(clock)

    /**
     * Тайтл в списках (каталог, поиск, подборки, папки) — без серий: `api/v1/items` отдаёт их только
     * в деталях. Фейк не подкладывает серии туда, где их нет на живом API, чтобы экран, по ошибке
     * опёршийся на них в списке, сломался и здесь, а не только в проде.
     */
    internal fun listItem(entry: CatalogEntry): Item =
        entry.item.copy(tracklist = emptyList(), inWatchlist = library.isInWatchlist(entry.id))

    /** Тайтл в деталях (`items/{id}`): серии с прогрессом и флаг watchlist на текущий момент. */
    internal fun detailedItem(entry: CatalogEntry): Item =
        entry.item.copy(tracklist = watch.withProgress(entry.item), inWatchlist = library.isInWatchlist(entry.id))

    internal fun itemPage(entries: List<CatalogEntry>, page: Int): ItemPage =
        ItemPage(
            items = entries.pageOf(page).map { listItem(it) },
            pagination = paginationOf(entries.size, page),
        )
}

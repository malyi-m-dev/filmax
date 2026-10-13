package com.filmax.data.fake.di

import com.filmax.core.domain.auth.AuthRepository
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.person.CastRepository
import com.filmax.core.domain.search.SearchRepository
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.data.fake.FakeAuthRepository
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeCastRepository
import com.filmax.data.fake.FakeCatalogRepository
import com.filmax.data.fake.FakeSearchRepository
import com.filmax.data.fake.FakeUserRepository
import com.filmax.data.fake.FakeWatchingRepository
import org.koin.dsl.module

/**
 * Подменяет сетевые репозитории kino.pub и TMDB фейками на общем [FakeBackend]. Грузится ПОСЛЕ
 * настоящих data-модулей: Koin 4 по умолчанию разрешает override, и более позднее определение
 * того же типа побеждает. Локальные репозитории (избранное, загрузки, настройки плеера) остаются
 * настоящими: сеть им не нужна, а избранное и так ведёт «Буду смотреть» через [UserRepository].
 *
 * Сетевые определения настоящих модулей (HttpClient, *Api) остаются в графе, но они ленивые и
 * без запросивших их репозиториев не создаются — в kino.pub мок-сборка не ходит.
 */
val fakeDataModule = module {
    single { FakeBackend() }
    single<AuthRepository> { FakeAuthRepository(backend = get()) }
    single<CatalogRepository> { FakeCatalogRepository(backend = get()) }
    single<SearchRepository> { FakeSearchRepository(backend = get()) }
    single<UserRepository> { FakeUserRepository(backend = get()) }
    single<WatchingRepository> { FakeWatchingRepository(backend = get()) }
    single<CastRepository> { FakeCastRepository(backend = get()) }
}

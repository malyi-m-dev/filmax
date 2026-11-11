package com.filmax.data.fake.di

import com.filmax.core.domain.auth.AuthRepository
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.feedback.FeedbackRepository
import com.filmax.core.domain.person.CastRepository
import com.filmax.core.domain.search.SearchRepository
import com.filmax.core.domain.source.ContentSource
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.core.domain.watchlist.WatchlistRepository
import com.filmax.data.fake.FakeAuthRepository
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeCastRepository
import com.filmax.data.fake.FakeCatalogRepository
import com.filmax.data.fake.FakeFeedbackRepository
import com.filmax.data.fake.FakeSearchRepository
import com.filmax.data.fake.FakeUserRepository
import com.filmax.data.fake.FakeWatchingRepository
import com.filmax.data.fake.FakeWatchlistRepository
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Подменяет сетевые репозитории kino.pub и TMDB фейками на общем [FakeBackend]. Грузится ПОСЛЕ
 * настоящих data-модулей: Koin 4 по умолчанию разрешает override, и более позднее определение
 * того же типа побеждает.
 *
 * Репозитории kino.pub подменяются под квалификатором источника: тот же ключ читают и
 * переключатель источника (data:source), и псевдоним без квалификатора. Поэтому мок-сборка
 * остаётся фейком для kino.pub, а «Домашний сервер» в ней — настоящий: UI входа и каталога
 * сервера можно проверить на эмуляторе против своего Jellyfin.
 *
 * «Буду смотреть» kino.pub тоже фейковый: настоящий ставит флаг через WatchingApi, а в сеть
 * мок-сборка не ходит. Локальные репозитории (настройки плеера, оформление) остаются настоящими.
 *
 * Сетевые определения настоящих модулей (HttpClient, *Api) остаются в графе, но они ленивые и
 * без запросивших их репозиториев не создаются — в kino.pub мок-сборка не ходит.
 */
val fakeDataModule = module {
    single { FakeBackend() }
    val kinopub = named(ContentSource.KINOPUB)
    single<AuthRepository>(kinopub) { FakeAuthRepository(backend = get()) }
    single<CatalogRepository>(kinopub) { FakeCatalogRepository(backend = get()) }
    single<SearchRepository>(kinopub) { FakeSearchRepository(backend = get()) }
    single<UserRepository>(kinopub) { FakeUserRepository(backend = get()) }
    single<WatchingRepository>(kinopub) { FakeWatchingRepository(backend = get()) }
    single<WatchlistRepository>(kinopub) { FakeWatchlistRepository(backend = get()) }
    single<CastRepository> { FakeCastRepository(backend = get()) }
    // Отзывы из мок-сборки в боевую базу Firestore не уходят.
    single<FeedbackRepository> { FakeFeedbackRepository(backend = get()) }
}

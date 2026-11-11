package com.filmax.shared

import com.filmax.core.domain.common.LastValueCache
import com.filmax.core.domain.usecase.auth.LogoutUseCase
import com.filmax.core.domain.usecase.auth.ObserveAuthStateUseCase
import com.filmax.core.domain.usecase.auth.PollForTokenUseCase
import com.filmax.core.domain.usecase.auth.RequestDeviceCodeUseCase
import com.filmax.core.domain.usecase.bookmarks.BookmarkFolders
import com.filmax.core.domain.usecase.home.GetHomeFeedUseCase
import com.filmax.core.domain.usecase.home.HomeFeed
import com.filmax.core.domain.usecase.watching.ToggleWatchedUseCase
import com.filmax.core.domain.usecase.watching.ToggleWatchlistUseCase
import com.filmax.core.network.di.networkModule
import com.filmax.core.network.di.platformNetworkModule
import com.filmax.data.auth.di.authModule
import com.filmax.data.catalog.di.catalogModule
import com.filmax.data.jellyfin.di.jellyfinModule
import com.filmax.data.search.di.searchModule
import com.filmax.data.source.di.sourceModule
import com.filmax.data.user.di.userModule
import com.filmax.data.watching.di.watchingModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * Use case'ы домена — один модуль на Android и iOS. Раньше Android регистрировал свои копии
 * (`app` и `feature:home`), и правка одной копии не доходила до другой платформы.
 */
val domainUseCaseModule = module {
    // Кэш последней ленты — single, чтобы переживать пересоздание use-case (офлайн-устойчивость #42).
    single { LastValueCache<HomeFeed>() }
    factory { GetHomeFeedUseCase(catalog = get(), watching = get(), cache = get()) }
    factory { ObserveAuthStateUseCase(get()) }
    factory { RequestDeviceCodeUseCase(get()) }
    factory { PollForTokenUseCase(get()) }
    factory { LogoutUseCase(get()) }
    factory { ToggleWatchedUseCase(get()) }
    factory { ToggleWatchlistUseCase(watchlist = get()) }
    factory { BookmarkFolders(user = get(), appearance = get()) }
}

/**
 * Data-слой обоих источников. Порядок важен: [sourceModule] — последним, он занимает ключи без
 * квалификатора переключателями между kino.pub и домашним сервером.
 */
val sharedDataModules = listOf(
    authModule,
    catalogModule,
    searchModule,
    userModule,
    watchingModule,
    jellyfinModule,
    sourceModule,
)

/** Все общие модули shared (сеть + data + domain UseCase). */
val sharedModules = listOf(networkModule, platformNetworkModule) + sharedDataModules + domainUseCaseModule

/**
 * Инициализация Koin из общего кода.
 * iOS вызывает `KoinKt.doInitKoin()` в `iOSApp.swift`; Android может переиспользовать,
 * передав feature-модули через [appDeclaration].
 */
fun doInitKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication =
    startKoin {
        appDeclaration()
        modules(sharedModules)
    }

package com.filmax.shared

import com.filmax.core.domain.usecase.auth.LogoutUseCase
import com.filmax.core.domain.usecase.auth.ObserveAuthStateUseCase
import com.filmax.core.domain.usecase.auth.PollForTokenUseCase
import com.filmax.core.domain.usecase.auth.RequestDeviceCodeUseCase
import com.filmax.core.domain.common.LastValueCache
import com.filmax.core.domain.usecase.home.GetHomeFeedUseCase
import com.filmax.core.domain.usecase.home.HomeFeed
import com.filmax.core.domain.usecase.watching.ToggleWatchedUseCase
import com.filmax.core.domain.usecase.watching.ToggleWatchlistUseCase
import com.filmax.core.network.di.networkModule
import com.filmax.core.network.di.platformNetworkModule
import com.filmax.data.auth.di.authModule
import com.filmax.data.catalog.di.catalogModule
import com.filmax.data.search.di.searchModule
import com.filmax.data.user.di.userModule
import com.filmax.data.watching.di.watchingModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/** UseCase-слой как Koin-модуль — общий для Android и iOS. */
val useCaseModule = module {
    // Кэш последней ленты — single, чтобы переживать пересоздание use-case (офлайн-устойчивость #42).
    single { LastValueCache<HomeFeed>() }
    factory { GetHomeFeedUseCase(catalog = get(), watching = get(), cache = get()) }
    factory { ObserveAuthStateUseCase(get()) }
    factory { RequestDeviceCodeUseCase(get()) }
    factory { PollForTokenUseCase(get()) }
    factory { LogoutUseCase(get()) }
    factory { ToggleWatchlistUseCase(get()) }
    factory { ToggleWatchedUseCase(get()) }
}

/** Все общие модули shared (domain UseCase + data + network). */
val sharedModules = listOf(
    networkModule,
    platformNetworkModule,
    authModule,
    catalogModule,
    searchModule,
    userModule,
    watchingModule,
    useCaseModule,
)

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

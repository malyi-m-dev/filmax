package com.filmax.core.network.di

import com.filmax.core.network.TokenStorage
import com.filmax.core.network.buildHttpClient
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Общий сетевой DI-модуль. Зависит от платформенных провайдеров [platformNetworkModule]
 * ([com.russhwolf.settings.Settings] и [io.ktor.client.engine.HttpClientEngine]).
 */
val networkModule = module {
    single { TokenStorage(get()) }
    single<HttpClient> {
        buildHttpClient(
            engine = get(),
            tokenStorage = get(),
            enableLogging = true,
        )
    }
}

/** Платформенные зависимости сети: хранилище настроек и HTTP-движок (+ инспектор на Android). */
expect val platformNetworkModule: Module

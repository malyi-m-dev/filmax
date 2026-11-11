package com.filmax.core.network.kinopub

import com.filmax.core.network.isDebugBuild
import io.ktor.client.HttpClient
import org.koin.dsl.module

/**
 * Сеть kino.pub: токены и клиент API. Клиент без квалификатора — kino.pub: его берут Api-классы
 * data-модулей kino.pub. Зависит от платформенных провайдеров
 * [com.filmax.core.network.di.platformNetworkModule] ([com.russhwolf.settings.Settings] и
 * [io.ktor.client.engine.HttpClientEngine]).
 */
val kinopubNetworkModule = module {
    single { TokenStorage(get()) }
    single<HttpClient> {
        buildKinopubHttpClient(
            engine = get(),
            tokenStorage = get(),
            // Только в debug: логи печатают URL, параметры и тела ответов.
            enableLogging = isDebugBuild,
        )
    }
}

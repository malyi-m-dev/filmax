package com.filmax.data.tmdb.di

import com.filmax.core.domain.person.CastRepository
import com.filmax.data.tmdb.TmdbCastRepositoryImpl
import com.filmax.data.tmdb.remote.TmdbApi
import org.koin.dsl.module

/**
 * Ключ TMDB приходит Koin-свойством `tmdbApiKey` (его выставляет `:app` из BuildConfig, который
 * читает `local.properties`) — так секрет не попадает в код модуля и в репозиторий. Нет ключа →
 * пустая строка → CastRepository просто возвращает пустой список.
 */
val tmdbModule = module {
    single { TmdbApi(engine = get(), apiKey = getProperty(TMDB_API_KEY_PROPERTY, "")) }
    single<CastRepository> { TmdbCastRepositoryImpl(api = get()) }
}

const val TMDB_API_KEY_PROPERTY = "tmdbApiKey"

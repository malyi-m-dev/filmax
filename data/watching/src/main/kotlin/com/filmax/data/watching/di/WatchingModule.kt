package com.filmax.data.watching.di

import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.data.watching.WatchingRepositoryImpl
import com.filmax.data.watching.remote.WatchingApi
import org.koin.dsl.module
import retrofit2.Retrofit

val watchingModule = module {
    single<WatchingApi> { get<Retrofit>().create(WatchingApi::class.java) }
    single<WatchingRepository> { WatchingRepositoryImpl(api = get()) }
}

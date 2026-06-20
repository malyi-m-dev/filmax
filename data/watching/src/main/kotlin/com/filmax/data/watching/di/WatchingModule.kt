package com.filmax.data.watching.di

import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.data.watching.WatchingRepositoryImpl
import com.filmax.data.watching.remote.WatchingApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WatchingApiModule {
    @Provides
    @Singleton
    fun provideWatchingApi(retrofit: Retrofit): WatchingApi =
        retrofit.create(WatchingApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class WatchingBindingModule {
    @Binds
    @Singleton
    internal abstract fun bindWatchingRepository(impl: WatchingRepositoryImpl): WatchingRepository
}

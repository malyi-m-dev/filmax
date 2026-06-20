package com.filmax.data.search.di

import com.filmax.core.domain.search.SearchRepository
import com.filmax.data.search.SearchRepositoryImpl
import com.filmax.data.search.remote.SearchApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SearchApiModule {
    @Provides
    @Singleton
    fun provideSearchApi(retrofit: Retrofit): SearchApi =
        retrofit.create(SearchApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SearchBindingModule {
    @Binds
    @Singleton
    internal abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository
}

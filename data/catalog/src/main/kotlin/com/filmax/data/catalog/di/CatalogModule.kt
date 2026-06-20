package com.filmax.data.catalog.di

import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.data.catalog.CatalogRepositoryImpl
import com.filmax.data.catalog.remote.CatalogApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CatalogApiModule {
    @Provides
    @Singleton
    fun provideCatalogApi(retrofit: Retrofit): CatalogApi =
        retrofit.create(CatalogApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class CatalogBindingModule {
    @Binds
    @Singleton
    internal abstract fun bindCatalogRepository(impl: CatalogRepositoryImpl): CatalogRepository
}

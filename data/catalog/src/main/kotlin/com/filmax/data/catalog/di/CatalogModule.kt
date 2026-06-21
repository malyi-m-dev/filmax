package com.filmax.data.catalog.di

import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.data.catalog.CatalogRepositoryImpl
import com.filmax.data.catalog.remote.CatalogApi
import org.koin.dsl.module
import retrofit2.Retrofit

val catalogModule = module {
    single<CatalogApi> { get<Retrofit>().create(CatalogApi::class.java) }
    single<CatalogRepository> { CatalogRepositoryImpl(api = get()) }
}

package com.filmax.data.catalog.di

import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.data.catalog.CatalogRepositoryImpl
import com.filmax.data.catalog.remote.CatalogApi
import org.koin.dsl.module

val catalogModule = module {
    single { CatalogApi(get()) }
    single<CatalogRepository> { CatalogRepositoryImpl(api = get()) }
}

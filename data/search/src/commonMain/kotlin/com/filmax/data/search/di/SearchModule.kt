package com.filmax.data.search.di

import com.filmax.core.domain.search.SearchRepository
import com.filmax.data.search.SearchRepositoryImpl
import com.filmax.data.search.remote.SearchApi
import org.koin.dsl.module

val searchModule = module {
    single { SearchApi(get()) }
    single<SearchRepository> { SearchRepositoryImpl(api = get()) }
}

package com.filmax.data.search.di

import com.filmax.core.domain.search.SearchRepository
import com.filmax.data.search.SearchRepositoryImpl
import com.filmax.data.search.remote.SearchApi
import org.koin.dsl.module
import retrofit2.Retrofit

val searchModule = module {
    single<SearchApi> { get<Retrofit>().create(SearchApi::class.java) }
    single<SearchRepository> { SearchRepositoryImpl(api = get()) }
}

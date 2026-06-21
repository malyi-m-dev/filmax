package com.filmax.data.auth.di

import com.filmax.core.domain.auth.AuthRepository
import com.filmax.data.auth.AuthRepositoryImpl
import com.filmax.data.auth.remote.AuthApi
import org.koin.dsl.module
import retrofit2.Retrofit

val authModule = module {
    single<AuthApi> { get<Retrofit>().create(AuthApi::class.java) }
    single<AuthRepository> { AuthRepositoryImpl(api = get(), tokenStorage = get()) }
}

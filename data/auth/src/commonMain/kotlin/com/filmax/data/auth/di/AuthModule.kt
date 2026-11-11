package com.filmax.data.auth.di

import com.filmax.core.domain.auth.AuthRepository
import com.filmax.core.network.di.kinopubSingle
import com.filmax.data.auth.AuthRepositoryImpl
import com.filmax.data.auth.remote.AuthApi
import org.koin.dsl.module

val authModule = module {
    single { AuthApi(get()) }
    kinopubSingle<AuthRepository> { AuthRepositoryImpl(api = get(), tokenStorage = get()) }
}

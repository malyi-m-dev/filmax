package com.filmax.data.user.di

import com.filmax.core.domain.user.UserRepository
import com.filmax.data.user.UserRepositoryImpl
import com.filmax.data.user.remote.UserApi
import org.koin.dsl.module
import retrofit2.Retrofit

val userModule = module {
    single<UserApi> { get<Retrofit>().create(UserApi::class.java) }
    single<UserRepository> { UserRepositoryImpl(api = get()) }
}

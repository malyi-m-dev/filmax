package com.filmax.data.auth.di

import com.filmax.core.domain.auth.AuthRepository
import com.filmax.data.auth.AuthRepositoryImpl
import com.filmax.data.auth.remote.AuthApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthApiModule {
    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthBindingModule {
    @Binds
    @Singleton
    internal abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}

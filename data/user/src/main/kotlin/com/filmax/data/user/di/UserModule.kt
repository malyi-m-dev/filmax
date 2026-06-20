package com.filmax.data.user.di

import com.filmax.core.domain.user.UserRepository
import com.filmax.data.user.UserRepositoryImpl
import com.filmax.data.user.remote.UserApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UserApiModule {
    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi =
        retrofit.create(UserApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class UserBindingModule {
    @Binds
    @Singleton
    internal abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}

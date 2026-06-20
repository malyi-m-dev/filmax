package com.filmax.core.network.di

import com.filmax.core.network.AuthInterceptor
import com.filmax.core.network.buildOkHttpClient
import com.filmax.core.network.buildRetrofit
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatedClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseUrl

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    @AuthenticatedClient
    fun provideOkHttpClient(interceptor: AuthInterceptor): OkHttpClient =
        buildOkHttpClient(interceptor, debug = true)

    @Provides
    @Singleton
    fun provideRetrofit(
        @AuthenticatedClient client: OkHttpClient,
    ): Retrofit = buildRetrofit(
        baseUrl = "https://smarttvcdn.online/",
        client = client,
    )
}

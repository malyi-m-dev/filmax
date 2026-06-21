package com.filmax.core.network.di

import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.filmax.core.network.AuthInterceptor
import com.filmax.core.network.TokenStorage
import com.filmax.core.network.buildOkHttpClient
import com.filmax.core.network.buildRetrofit
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit

val networkModule = module {
    single { TokenStorage(androidContext()) }
    single { AuthInterceptor(get()) }
    single { ChuckerInterceptor.Builder(androidContext()).build() }
    single<OkHttpClient> {
        buildOkHttpClient(
            authInterceptor = get(),
            networkInspector = get<ChuckerInterceptor>(),
            debug = true,
        )
    }
    single<Retrofit> {
        buildRetrofit(
            baseUrl = "https://smarttvcdn.online/",
            client = get(),
        )
    }
}

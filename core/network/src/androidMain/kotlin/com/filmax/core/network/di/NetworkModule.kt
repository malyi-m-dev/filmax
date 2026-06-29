package com.filmax.core.network.di

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.filmax.core.network.TokenStorage
import com.filmax.core.network.buildHttpClient
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val networkModule = module {
    single<Settings> {
        SharedPreferencesSettings(
            androidContext().getSharedPreferences("filmax_tokens", Context.MODE_PRIVATE),
        )
    }
    single { TokenStorage(get()) }

    single { ChuckerInterceptor.Builder(androidContext()).build() }
    single<HttpClientEngine> {
        OkHttp.create { addInterceptor(get<ChuckerInterceptor>()) }
    }
    single<HttpClient> {
        buildHttpClient(
            engine = get(),
            tokenStorage = get(),
            enableLogging = true,
        )
    }
}

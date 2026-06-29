package com.filmax.core.network.di

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.filmax.core.network.AuthInterceptor
import com.filmax.core.network.BASE_URL
import com.filmax.core.network.TokenStorage
import com.filmax.core.network.buildHttpClient
import com.filmax.core.network.buildOkHttpClient
import com.filmax.core.network.buildRetrofit
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit

val networkModule = module {
    single<Settings> {
        SharedPreferencesSettings(
            androidContext().getSharedPreferences("filmax_tokens", Context.MODE_PRIVATE),
        )
    }
    single { TokenStorage(get()) }

    // --- Ktor (целевой стек; data-модули переключаются в Фазе 3) ---
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

    // --- Retrofit/OkHttp (временно, до миграции data-слоя на Ktor) ---
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
            baseUrl = BASE_URL,
            client = get(),
        )
    }
}

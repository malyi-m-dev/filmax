package com.filmax.core.network

import android.util.Log
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

private const val TIMEOUT_SEC = 60L

val networkJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

fun buildOkHttpClient(
    authInterceptor: AuthInterceptor,
    networkInspector: Interceptor? = null,
    debug: Boolean = false,
): OkHttpClient = OkHttpClient.Builder()
    .addInterceptor(authInterceptor)
    .apply { networkInspector?.let { addInterceptor(it) } }
    .apply {
        if (debug) addInterceptor(
            HttpLoggingInterceptor {
                android.util.Log.d("OkHttp", it)
                println("kekes log http $it")
            }
                .apply { level = HttpLoggingInterceptor.Level.BODY },
        )
    }
    .connectTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
    .readTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
    .writeTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
    .build()

fun buildRetrofit(baseUrl: String, client: OkHttpClient): Retrofit =
    Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(networkJson.asConverterFactory("application/json; charset=UTF8".toMediaType()))
        .build()

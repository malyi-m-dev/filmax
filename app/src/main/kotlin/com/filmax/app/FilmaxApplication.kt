package com.filmax.app

import android.app.Application
import android.content.pm.PackageManager
import com.filmax.app.di.appModule
import com.filmax.core.domain.common.ErrorReporting
import com.filmax.core.network.di.networkModule
import com.filmax.core.network.di.platformNetworkModule
import com.filmax.data.auth.di.authModule
import com.filmax.data.catalog.di.catalogModule
import com.filmax.data.search.di.searchModule
import com.filmax.data.tmdb.di.TMDB_API_KEY_PROPERTY
import com.filmax.data.tmdb.di.tmdbModule
import com.filmax.data.user.di.userModule
import com.filmax.data.watching.di.watchingModule
import com.filmax.feature.collections.common.di.collectionsModule
import com.filmax.feature.details.common.di.detailsModule
import com.filmax.feature.home.common.di.homeModule
import com.filmax.feature.library.common.di.libraryModule
import com.filmax.feature.onboarding.common.di.onboardingModule
import com.filmax.feature.player.common.di.playerModule
import com.filmax.feature.profile.common.di.profileModule
import com.filmax.feature.search.common.di.searchFeatureModule
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class FilmaxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initErrorReporting()
        seedDemoTokenIfNeeded()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@FilmaxApplication)
            // Ключ TMDB отдаём модулю свойством, а не в коде: секрет живёт в BuildConfig (из
            // local.properties), а data:tmdb читает его через getProperty.
            properties(mapOf(TMDB_API_KEY_PROPERTY to BuildConfig.TMDB_API_KEY))
            modules(
                // core / data
                networkModule,
                platformNetworkModule,
                authModule,
                catalogModule,
                searchModule,
                userModule,
                watchingModule,
                tmdbModule,
                // features
                onboardingModule,
                homeModule,
                searchFeatureModule,
                collectionsModule,
                libraryModule,
                profileModule,
                detailsModule,
                playerModule,
                // app
                appModule,
            )
        }
    }

    /**
     * Включает телеметрию ошибок. Сборка без app/google-services.json (локальная/PR-CI) Firebase
     * не сконфигурирована — initializeApp вернёт null, и репортинг остаётся no-op из ErrorReporting.
     */
    private fun initErrorReporting() {
        FirebaseApp.initializeApp(this) ?: return
        val crashlytics = FirebaseCrashlytics.getInstance()
        // Debug-сессии — шум разработки, их не собираем; release/demo шлют крэши и non-fatal'ы.
        crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        // Один APK на оба форм-фактора — в отчётах различаем их так же, как MainActivity выбирает UI.
        val isTv = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        crashlytics.setCustomKey("form_factor", if (isTv) "tv" else "mobile")
        ErrorReporting.reporter = CrashlyticsErrorReporter(crashlytics)
    }

    /**
     * Demo-сборка стартует авторизованной: если в BuildConfig зашит токен (только build type `demo`)
     * и хранилище ещё пустое — засеваем те же SharedPreferences `filmax_tokens`, что читает
     * TokenStorage при создании. Так demo-билд открывается без входа на любом устройстве. В
     * release/debug оба токена пустые — метод сразу выходит и ничего не трогает.
     */
    private fun seedDemoTokenIfNeeded() {
        val access = BuildConfig.DEMO_ACCESS_TOKEN
        val refresh = BuildConfig.DEMO_REFRESH_TOKEN
        if (access.isBlank() || refresh.isBlank()) return
        val prefs = getSharedPreferences("filmax_tokens", MODE_PRIVATE)
        if (prefs.getString("access_token", null) != null) return
        prefs.edit()
            .putString("access_token", access)
            .putString("refresh_token", refresh)
            .apply()
    }
}

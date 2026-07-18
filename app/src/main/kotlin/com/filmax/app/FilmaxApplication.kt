package com.filmax.app

import android.app.Application
import com.filmax.app.di.appModule
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
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class FilmaxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
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
}

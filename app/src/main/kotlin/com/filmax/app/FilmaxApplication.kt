package com.filmax.app

import android.app.Application
import com.filmax.app.di.appModule
import com.filmax.core.network.di.networkModule
import com.filmax.core.network.di.platformNetworkModule
import com.filmax.data.auth.di.authModule
import com.filmax.data.catalog.di.catalogModule
import com.filmax.data.search.di.searchModule
import com.filmax.data.user.di.userModule
import com.filmax.data.watching.di.watchingModule
import com.filmax.feature.collections.di.collectionsModule
import com.filmax.feature.details.di.detailsModule
import com.filmax.feature.home.di.homeModule
import com.filmax.feature.library.di.libraryModule
import com.filmax.feature.onboarding.di.onboardingModule
import com.filmax.feature.player.di.playerModule
import com.filmax.feature.profile.di.profileModule
import com.filmax.feature.search.di.searchFeatureModule
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
            modules(
                // core / data
                networkModule,
                platformNetworkModule,
                authModule,
                catalogModule,
                searchModule,
                userModule,
                watchingModule,
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

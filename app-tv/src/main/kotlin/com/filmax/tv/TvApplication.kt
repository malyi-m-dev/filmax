package com.filmax.tv

import android.app.Application
import com.filmax.core.network.di.networkModule
import com.filmax.core.network.di.platformNetworkModule
import com.filmax.data.auth.di.authModule
import com.filmax.data.catalog.di.catalogModule
import com.filmax.data.user.di.userModule
import com.filmax.data.watching.di.watchingModule
import com.filmax.feature.home.di.homeModule
import com.filmax.feature.onboarding.di.onboardingModule
import com.filmax.tv.di.tvAppModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class TvApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@TvApplication)
            modules(
                // core / data
                networkModule,
                platformNetworkModule,
                authModule,
                catalogModule,
                userModule,
                watchingModule,
                // shared MVI logic
                onboardingModule,
                homeModule,
                // app-tv
                tvAppModule,
            )
        }
    }
}

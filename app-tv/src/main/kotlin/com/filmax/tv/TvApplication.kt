package com.filmax.tv

import android.app.Application
import com.filmax.core.network.di.networkModule
import com.filmax.core.network.di.platformNetworkModule
import com.filmax.data.auth.di.authModule
import com.filmax.data.catalog.di.catalogModule
import com.filmax.data.search.di.searchModule
import com.filmax.data.user.di.userModule
import com.filmax.data.watching.di.watchingModule
import com.filmax.feature.details.di.detailsModule
import com.filmax.feature.home.di.homeModule
import com.filmax.feature.library.di.libraryModule
import com.filmax.feature.onboarding.di.onboardingModule
import com.filmax.feature.player.di.playerModule
import com.filmax.feature.profile.di.profileModule
import com.filmax.feature.search.di.searchFeatureModule
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
                searchModule,
                userModule,
                watchingModule,
                // переиспользуемая MVI-логика мобильных фич
                onboardingModule,
                homeModule,
                searchFeatureModule,
                libraryModule,
                profileModule,
                detailsModule,
                playerModule,
                // app-tv
                tvAppModule,
            )
        }
    }
}

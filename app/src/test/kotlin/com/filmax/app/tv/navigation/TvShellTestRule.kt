package com.filmax.app.tv.navigation

import android.app.Application
import android.content.pm.PackageManager
import com.filmax.app.di.appModule
import com.filmax.app.di.useCaseModule
import com.filmax.core.navigation.di.navigationModule
import com.filmax.core.network.di.networkModule
import com.filmax.core.network.di.platformNetworkModule
import com.filmax.data.auth.di.authModule
import com.filmax.data.catalog.di.catalogModule
import com.filmax.data.fake.FakeBackend
import com.filmax.data.fake.FakeLatency
import com.filmax.data.fake.di.fakeDataModule
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
import org.junit.rules.ExternalResource
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * Граф приложения для UI-тестов ТВ-оболочки — тот же, что у мок-сборки: настоящие модули фич,
 * репозитории kino.pub подменены фейками, ответы без задержки. Устройство — телевизор
 * (FEATURE_LEANBACK). Koin поднимается перед тестом и гасится после: у каждого теста свой аккаунт.
 */
class TvShellTestRule : ExternalResource() {

    override fun before() {
        val application = RuntimeEnvironment.getApplication()
        shadowOf(application.packageManager).setSystemFeature(PackageManager.FEATURE_LEANBACK, true)
        startKoin {
            androidContext(application as Application)
            properties(mapOf(TMDB_API_KEY_PROPERTY to ""))
            modules(
                listOf(navigationModule, networkModule, platformNetworkModule) +
                    listOf(authModule, catalogModule, searchModule, userModule, watchingModule, tmdbModule) +
                    listOf(fakeDataModule, module { single { FakeBackend(latency = FakeLatency.None) } }) +
                    listOf(
                        useCaseModule,
                        onboardingModule,
                        homeModule,
                        searchFeatureModule,
                        collectionsModule,
                        libraryModule,
                        profileModule,
                        detailsModule,
                        playerModule,
                        appModule,
                    ),
            )
        }
    }

    override fun after() {
        stopKoin()
    }
}

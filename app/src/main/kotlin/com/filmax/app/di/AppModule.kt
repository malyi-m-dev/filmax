package com.filmax.app.di

import com.filmax.app.navigation.RootScreenModel
import com.filmax.app.update.AppUpdateScreenModel
import com.filmax.app.update.GitHubUpdateRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    viewModelOf(::RootScreenModel)
    single { GitHubUpdateRepository(androidContext()) }
    viewModelOf(::AppUpdateScreenModel)
}

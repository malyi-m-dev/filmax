package com.filmax.app.di

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.filmax.app.BuildConfig
import com.filmax.app.navigation.RootScreenModel
import com.filmax.app.update.AppUpdateScreenModel
import com.filmax.app.update.GitHubUpdateRepository
import com.filmax.core.domain.feedback.ClientInfo
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Диспетчер IO раздаётся отсюда, а не берётся по месту использования: с инжектом его можно
// подменить в тестах. DI-модуль — единственная легитимная точка прямого Dispatchers.IO,
// но правило исключений не делает, поэтому Suppress именно здесь.
@Suppress("InjectDispatcher")
val appModule = module {
    viewModelOf(::RootScreenModel)
    single { GitHubUpdateRepository(androidContext(), Dispatchers.IO) }
    viewModel { AppUpdateScreenModel(get(), Dispatchers.IO) }
    single { clientInfo(androidContext()) }
}

/** Версия, форм-фактор и устройство — для отзывов: по ним видно, на чём человек смотрит. */
private fun clientInfo(context: Context) = ClientInfo(
    appVersion = BuildConfig.VERSION_NAME,
    isTv = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK),
    device = "${Build.MANUFACTURER} ${Build.MODEL}",
    os = "Android ${Build.VERSION.RELEASE}",
)

package com.filmax.app.di

import com.filmax.app.BuildConfig
import com.filmax.core.domain.feedback.FeedbackPrompts
import org.koin.dsl.module

/**
 * Use case'ы, которым нужно Android-окружение (версия из BuildConfig, системные часы). Доменные —
 * в `domainUseCaseModule` из :shared, общем с iOS.
 */
val useCaseModule = module {
    // Окно оценки спрашивают Главная (после обновления), плеер (после досмотра) и само окно.
    factory { FeedbackPrompts(store = get(), appVersion = BuildConfig.VERSION_NAME, now = System::currentTimeMillis) }
}

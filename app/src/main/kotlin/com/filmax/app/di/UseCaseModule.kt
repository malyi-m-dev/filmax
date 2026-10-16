package com.filmax.app.di

import com.filmax.app.BuildConfig
import com.filmax.core.domain.feedback.FeedbackPrompts
import com.filmax.core.domain.usecase.bookmarks.BookmarkFolders
import com.filmax.core.domain.usecase.watching.RemoveFromWatchLaterUseCase
import com.filmax.core.domain.usecase.watching.ToggleWatchLaterUseCase
import org.koin.dsl.module

/**
 * Доменные use case'ы, общие для нескольких фич: регистрация в одном месте, а не в модуле каждой
 * фичи — иначе определения дублировались бы и перекрывали друг друга.
 */
val useCaseModule = module {
    factory { ToggleWatchLaterUseCase(favorites = get(), watching = get()) }
    factory { RemoveFromWatchLaterUseCase(favorites = get(), watching = get()) }
    factory { BookmarkFolders(user = get(), appearance = get()) }
    // Окно оценки спрашивают Главная (после обновления), плеер (после досмотра) и само окно.
    factory { FeedbackPrompts(store = get(), appVersion = BuildConfig.VERSION_NAME, now = System::currentTimeMillis) }
}

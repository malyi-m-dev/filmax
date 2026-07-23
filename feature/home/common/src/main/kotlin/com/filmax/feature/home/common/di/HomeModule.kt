package com.filmax.feature.home.common.di

import com.filmax.core.domain.common.LastValueCache
import com.filmax.core.domain.usecase.home.GetHomeFeedUseCase
import com.filmax.core.domain.usecase.home.HomeFeed
import com.filmax.feature.home.common.HomeScreenModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val homeModule = module {
    // Кэш последней ленты — single, чтобы переживать пересоздание use-case (офлайн-устойчивость #42).
    single { LastValueCache<HomeFeed>() }
    factory { GetHomeFeedUseCase(catalog = get(), watching = get(), cache = get()) }
    viewModelOf(::HomeScreenModel)
}

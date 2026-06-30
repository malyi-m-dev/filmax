package com.filmax.feature.home.common.di

import com.filmax.core.domain.usecase.home.GetHomeFeedUseCase
import com.filmax.core.domain.usecase.watching.ToggleWatchlistUseCase
import com.filmax.feature.home.common.HomeScreenModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val homeModule = module {
    factory { GetHomeFeedUseCase(catalog = get(), watching = get()) }
    factory { ToggleWatchlistUseCase(get()) }
    viewModelOf(::HomeScreenModel)
}

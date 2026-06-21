package com.filmax.feature.home.di

import com.filmax.feature.home.HomeScreenModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val homeModule = module {
    viewModelOf(::HomeScreenModel)
}

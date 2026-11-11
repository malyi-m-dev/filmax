package com.filmax.feature.home.common.di

import com.filmax.feature.home.common.HomeScreenModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val homeModule = module {
    viewModelOf(::HomeScreenModel)
}

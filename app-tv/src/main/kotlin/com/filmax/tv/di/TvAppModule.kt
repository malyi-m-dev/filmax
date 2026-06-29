package com.filmax.tv.di

import com.filmax.tv.navigation.TvRootScreenModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val tvAppModule = module {
    viewModelOf(::TvRootScreenModel)
}

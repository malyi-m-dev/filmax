package com.filmax.app.di

import com.filmax.app.navigation.RootViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    viewModelOf(::RootViewModel)
}

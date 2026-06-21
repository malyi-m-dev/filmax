package com.filmax.feature.profile.di

import com.filmax.feature.profile.ProfileScreenModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val profileModule = module {
    viewModelOf(::ProfileScreenModel)
}
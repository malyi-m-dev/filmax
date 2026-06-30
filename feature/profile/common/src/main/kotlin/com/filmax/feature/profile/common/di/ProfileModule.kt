package com.filmax.feature.profile.common.di

import com.filmax.feature.profile.common.ProfileScreenModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val profileModule = module {
    viewModelOf(::ProfileScreenModel)
}

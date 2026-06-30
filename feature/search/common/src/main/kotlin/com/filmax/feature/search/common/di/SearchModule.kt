package com.filmax.feature.search.common.di

import com.filmax.feature.search.common.SearchScreenModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val searchFeatureModule = module {
    viewModelOf(::SearchScreenModel)
}

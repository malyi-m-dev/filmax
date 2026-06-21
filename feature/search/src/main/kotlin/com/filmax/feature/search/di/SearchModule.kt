package com.filmax.feature.search.di

import com.filmax.feature.search.SearchScreenModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val searchFeatureModule = module {
    viewModelOf(::SearchScreenModel)
}

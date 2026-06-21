package com.filmax.feature.search.di

import com.filmax.feature.search.SearchViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val searchFeatureModule = module {
    viewModelOf(::SearchViewModel)
}

package com.filmax.feature.collections.di

import com.filmax.feature.collections.CollectionsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val collectionsModule = module {
    viewModelOf(::CollectionsViewModel)
}

package com.filmax.feature.details.di

import com.filmax.feature.details.DetailsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val detailsModule = module {
    viewModelOf(::DetailsViewModel)
}

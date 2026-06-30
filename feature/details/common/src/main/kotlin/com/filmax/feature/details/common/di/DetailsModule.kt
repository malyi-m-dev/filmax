package com.filmax.feature.details.common.di

import com.filmax.feature.details.common.DetailsScreenModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val detailsModule = module {
    viewModelOf(::DetailsScreenModel)
}

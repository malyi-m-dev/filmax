package com.filmax.feature.library.common.di

import com.filmax.feature.library.common.LibraryScreenModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val libraryModule = module {
    viewModelOf(::LibraryScreenModel)
}

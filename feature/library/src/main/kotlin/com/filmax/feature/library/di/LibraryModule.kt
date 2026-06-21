package com.filmax.feature.library.di

import com.filmax.feature.library.LibraryScreenModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val libraryModule = module {
    viewModelOf(::LibraryScreenModel)
}

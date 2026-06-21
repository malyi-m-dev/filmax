package com.filmax.feature.player.di

import com.filmax.feature.player.PlayerScreenModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val playerModule = module {
    viewModelOf(::PlayerScreenModel)
}

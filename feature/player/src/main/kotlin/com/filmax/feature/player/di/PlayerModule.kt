package com.filmax.feature.player.di

import com.filmax.feature.player.PlayerViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val playerModule = module {
    viewModelOf(::PlayerViewModel)
}

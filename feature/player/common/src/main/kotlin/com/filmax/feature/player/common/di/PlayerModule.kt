package com.filmax.feature.player.common.di

import com.filmax.feature.player.common.PlayerScreenModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val playerModule = module {
    viewModelOf(::PlayerScreenModel)
}

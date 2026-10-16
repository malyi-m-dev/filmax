package com.filmax.feature.player.common.di

import com.filmax.feature.player.common.PlayerRepositories
import com.filmax.feature.player.common.PlayerScreenModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val playerModule = module {
    factoryOf(::PlayerRepositories)
    viewModelOf(::PlayerScreenModel)
}

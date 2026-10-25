package com.filmax.feature.tvlink.common.di

import com.filmax.feature.tvlink.common.RemoteKeeperScreenModel
import com.filmax.feature.tvlink.common.RemoteScreenModel
import com.filmax.feature.tvlink.common.TvLinkHostScreenModel
import com.filmax.feature.tvlink.common.TvPickerScreenModel
import com.filmax.feature.tvlink.common.TvRemoteInfoScreenModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Поиск, пульт и приёмник даёт data:tvlink (`tvLinkModule`). */
val tvLinkFeatureModule = module {
    viewModelOf(::TvPickerScreenModel)
    viewModelOf(::RemoteScreenModel)
    viewModelOf(::RemoteKeeperScreenModel)
    viewModelOf(::TvLinkHostScreenModel)
    viewModelOf(::TvRemoteInfoScreenModel)
}

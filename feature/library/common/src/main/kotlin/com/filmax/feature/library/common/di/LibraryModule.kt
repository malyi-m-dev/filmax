package com.filmax.feature.library.common.di

import com.filmax.feature.library.common.FolderScreenModel
import com.filmax.feature.library.common.MineScreenModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import java.time.Clock

val libraryModule = module {
    viewModelOf(::FolderScreenModel)
    // Часы — системные: история делится на дни по часовому поясу устройства.
    viewModel {
        MineScreenModel(
            watching = get(),
            favorites = get(),
            folders = get(),
            removeFromWatchLater = get(),
            clock = Clock.systemDefaultZone(),
        )
    }
}

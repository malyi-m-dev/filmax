package com.filmax.data.watching.di

import com.filmax.core.domain.downloads.DownloadsRepository
import com.filmax.core.domain.favorites.FavoritesRepository
import com.filmax.core.domain.playback.PlaybackSettingsRepository
import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.data.watching.DownloadsRepositoryImpl
import com.filmax.data.watching.FavoritesRepositoryImpl
import com.filmax.data.watching.PlaybackSettingsRepositoryImpl
import com.filmax.data.watching.WatchingRepositoryImpl
import com.filmax.data.watching.remote.WatchingApi
import org.koin.dsl.module

val watchingModule = module {
    single { WatchingApi(get()) }
    single<WatchingRepository> { WatchingRepositoryImpl(api = get()) }
    single<DownloadsRepository> { DownloadsRepositoryImpl(settings = get()) }
    single<FavoritesRepository> { FavoritesRepositoryImpl(settings = get()) }
    single<PlaybackSettingsRepository> { PlaybackSettingsRepositoryImpl(storage = get()) }
}

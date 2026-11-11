package com.filmax.data.watching.di

import com.filmax.core.domain.favorites.FavoritesRepository
import com.filmax.core.domain.playback.PlaybackSettingsRepository
import com.filmax.core.domain.settings.ThemeSettingsRepository
import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.core.network.di.KinopubSource
import com.filmax.core.network.di.kinopubSingle
import com.filmax.data.watching.FavoritesRepositoryImpl
import com.filmax.data.watching.PlaybackSettingsRepositoryImpl
import com.filmax.data.watching.ThemeSettingsRepositoryImpl
import com.filmax.data.watching.WatchingRepositoryImpl
import com.filmax.data.watching.remote.WatchingApi
import org.koin.dsl.module

val watchingModule = module {
    single { WatchingApi(get()) }
    kinopubSingle<WatchingRepository> { WatchingRepositoryImpl(api = get()) }
    // «Буду смотреть» kino.pub — серверная папка-закладка, поэтому ему нужен UserRepository
    // именно kino.pub (по квалификатору): без него при домашнем сервере список ушёл бы искать
    // папку на сервере, где папок нет.
    kinopubSingle<FavoritesRepository> {
        FavoritesRepositoryImpl(userRepository = get(KinopubSource), settings = get())
    }
    single<PlaybackSettingsRepository> { PlaybackSettingsRepositoryImpl(storage = get()) }
    // Палитра ТВ — такая же локальная настройка устройства, как и настройки воспроизведения.
    single<ThemeSettingsRepository> { ThemeSettingsRepositoryImpl(settings = get()) }
}

package com.filmax.data.watching.di

import com.filmax.core.domain.auth.AuthRepository
import com.filmax.core.domain.playback.PlaybackSettingsRepository
import com.filmax.core.domain.settings.ThemeSettingsRepository
import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.core.domain.watchlist.WatchlistRepository
import com.filmax.core.network.kinopub.KinopubSource
import com.filmax.core.network.kinopub.kinopubSingle
import com.filmax.data.watching.KinopubWatchlistRepository
import com.filmax.data.watching.PlaybackSettingsRepositoryImpl
import com.filmax.data.watching.ThemeSettingsRepositoryImpl
import com.filmax.data.watching.WatchingRepositoryImpl
import com.filmax.data.watching.remote.WatchingApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

val watchingModule = module {
    single { WatchingApi(get()) }
    kinopubSingle<WatchingRepository> { WatchingRepositoryImpl(api = get()) }
    // «Буду смотреть» kino.pub — серверная папка-закладка, поэтому ему нужны UserRepository и
    // вход именно kino.pub (по квалификатору): без него при домашнем сервере список ушёл бы искать
    // папку на сервере, где папок нет. Область — на всё приложение: список живёт дольше экранов.
    kinopubSingle<WatchlistRepository> {
        KinopubWatchlistRepository(
            folders = get(KinopubSource),
            api = get(),
            settings = get(),
            signedIn = get<AuthRepository>(KinopubSource).isAuthenticated,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )
    }
    single<PlaybackSettingsRepository> { PlaybackSettingsRepositoryImpl(storage = get()) }
    // Палитра ТВ — такая же локальная настройка устройства, как и настройки воспроизведения.
    single<ThemeSettingsRepository> { ThemeSettingsRepositoryImpl(settings = get()) }
}

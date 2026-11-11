package com.filmax.data.jellyfin.di

import com.filmax.core.domain.auth.AuthRepository
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.feedback.ClientInfo
import com.filmax.core.domain.playback.StreamRepository
import com.filmax.core.domain.search.SearchRepository
import com.filmax.core.domain.source.ContentSource
import com.filmax.core.domain.source.HomeServerRepository
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.watching.WatchingRepository
import com.filmax.core.domain.watchlist.WatchlistRepository
import com.filmax.core.network.di.DeviceCache
import com.filmax.data.jellyfin.JellyfinAuthRepository
import com.filmax.data.jellyfin.JellyfinCatalogRepository
import com.filmax.data.jellyfin.JellyfinContext
import com.filmax.data.jellyfin.JellyfinDiscovery
import com.filmax.data.jellyfin.JellyfinIds
import com.filmax.data.jellyfin.JellyfinSearchRepository
import com.filmax.data.jellyfin.JellyfinServerRepository
import com.filmax.data.jellyfin.JellyfinSession
import com.filmax.data.jellyfin.JellyfinStreamRepository
import com.filmax.data.jellyfin.JellyfinUserRepository
import com.filmax.data.jellyfin.JellyfinVideos
import com.filmax.data.jellyfin.JellyfinWatchingRepository
import com.filmax.data.jellyfin.JellyfinWatchlistRepository
import com.filmax.data.jellyfin.remote.JellyfinApi
import com.filmax.data.jellyfin.remote.JellyfinClientInfo
import com.filmax.data.jellyfin.remote.jellyfinHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Свой HTTP-клиент сервера — под квалификатором, чтобы не занять ключ клиента kino.pub. */
private val JellyfinHttp = named("jellyfin_http")

/**
 * Домашний сервер. Репозитории контрактов — под квалификатором [ContentSource.JELLYFIN]: экраны
 * получают их через переключатель источника (data:source), а не напрямую. Вход на сервер
 * ([HomeServerRepository]) — без квалификатора: он есть только у сервера.
 *
 * Диспетчер и фоновая область создаются здесь: DI-модуль — единственное место, где их берут
 * напрямую, в классах они инжектятся.
 */
// Единственная точка прямого Dispatchers в модуле (см. KDoc), правило исключений не делает.
@Suppress("InjectDispatcher")
val jellyfinModule = module {
    single { JellyfinSession(settings = get()) }
    single { JellyfinIds(settings = get(DeviceCache)) }
    single(JellyfinHttp) {
        val client = get<ClientInfo>()
        jellyfinHttpClient(
            engine = get(),
            session = get(),
            client = JellyfinClientInfo(device = client.device, version = client.appVersion),
        )
    }
    single { JellyfinApi(client = get(JellyfinHttp), session = get()) }
    single { JellyfinContext(api = get(), ids = get(), session = get()) }
    single { JellyfinVideos(api = get(), ids = get()) }
    single { JellyfinDiscovery(dispatcher = Dispatchers.Default) }
    single<HomeServerRepository> { JellyfinServerRepository(api = get(), session = get(), discovery = get()) }

    val homeServer = named(ContentSource.JELLYFIN)
    single<AuthRepository>(homeServer) { JellyfinAuthRepository(context = get()) }
    single<CatalogRepository>(homeServer) { JellyfinCatalogRepository(context = get(), videos = get()) }
    single<SearchRepository>(homeServer) { JellyfinSearchRepository(context = get()) }
    single<WatchingRepository>(homeServer) { JellyfinWatchingRepository(context = get(), videos = get()) }
    single<UserRepository>(homeServer) { JellyfinUserRepository(context = get()) }
    single<WatchlistRepository>(homeServer) {
        JellyfinWatchlistRepository(context = get(), scope = CoroutineScope(SupervisorJob() + Dispatchers.Default))
    }
    single<StreamRepository>(homeServer) {
        JellyfinStreamRepository(context = get(), videos = get(), tv = get<ClientInfo>().isTv)
    }
}

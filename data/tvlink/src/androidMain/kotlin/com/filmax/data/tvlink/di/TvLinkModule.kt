package com.filmax.data.tvlink.di

import com.filmax.core.domain.tvlink.TvDiscovery
import com.filmax.core.domain.tvlink.TvLinkReceiver
import com.filmax.core.domain.tvlink.TvRemote
import com.filmax.data.tvlink.TvLinkAccount
import com.filmax.data.tvlink.TvLinkClient
import com.filmax.data.tvlink.TvLinkDiscovery
import com.filmax.data.tvlink.TvLinkMemory
import com.filmax.data.tvlink.TvLinkServer
import com.filmax.data.tvlink.android.AndroidLinkCrypto
import com.filmax.data.tvlink.android.AndroidLocalDevice
import com.filmax.data.tvlink.android.NsdAdvertiser
import com.filmax.data.tvlink.android.NsdBrowser
import com.filmax.data.tvlink.platform.LocalDevice
import com.filmax.data.tvlink.platform.ServiceAdvertiser
import com.filmax.data.tvlink.platform.ServiceBrowser
import com.filmax.data.tvlink.security.LinkCrypto
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Связь телефона с ТВ. Оба конца в одном APK: телефон пользуется поиском и пультом, ТВ — приёмником,
 * и кто чем пользуется, решает граф навигации. Не поднятый приёмник ничего не слушает, а пульт без
 * подключения — просто объект. Запомненный ТВ и свой id лежат в общих настройках устройства
 * ([com.russhwolf.settings.Settings] из core:network).
 *
 * Сокеты блокируют поток на время ожидания — им нужен IO-диспетчер; DI-модуль — единственная
 * легитимная точка прямого Dispatchers.IO.
 */
@Suppress("InjectDispatcher")
val tvLinkModule = module {
    single<LinkCrypto> { AndroidLinkCrypto() }
    single<LocalDevice> { AndroidLocalDevice(androidContext()) }
    single<ServiceAdvertiser> { NsdAdvertiser(androidContext()) }
    single<ServiceBrowser> { NsdBrowser(androidContext()) }
    single { TvLinkAccount(users = get(), crypto = get()) }
    single { TvLinkMemory(settings = get(), crypto = get()) }
    single<TvDiscovery> { TvLinkDiscovery(browser = get(), account = get()) }
    single<TvRemote> {
        TvLinkClient(account = get(), device = get(), memory = get(), browser = get(), dispatcher = Dispatchers.IO)
    }
    single<TvLinkReceiver> {
        TvLinkServer(account = get(), advertiser = get(), device = get(), memory = get(), dispatcher = Dispatchers.IO)
    }
}

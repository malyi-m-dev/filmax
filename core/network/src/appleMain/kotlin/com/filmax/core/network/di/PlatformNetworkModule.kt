package com.filmax.core.network.di

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

@OptIn(ExperimentalSettingsImplementation::class)
actual val platformNetworkModule: Module = module {
    // Токены на iOS хранятся только в Keychain.
    single<Settings> { KeychainSettings(service = "filmax_tokens") }
    // Кэши — в UserDefaults: Keychain для секретов, а не для растущих таблиц.
    single<Settings>(DeviceCache) { NSUserDefaultsSettings(NSUserDefaults(suiteName = "filmax_cache")) }
    single<HttpClientEngine> { Darwin.create() }
}

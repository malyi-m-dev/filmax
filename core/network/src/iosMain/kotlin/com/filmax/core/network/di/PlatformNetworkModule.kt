package com.filmax.core.network.di

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.module

@OptIn(ExperimentalSettingsImplementation::class)
actual val platformNetworkModule: Module = module {
    // Токены на iOS хранятся только в Keychain.
    single<Settings> { KeychainSettings(service = "filmax_tokens") }
    single<HttpClientEngine> { Darwin.create() }
}

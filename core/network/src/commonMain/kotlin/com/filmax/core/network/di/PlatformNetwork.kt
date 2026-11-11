package com.filmax.core.network.di

import org.koin.core.module.Module
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named

/**
 * Платформенные зависимости сети: хранилища и HTTP-движок (+ инспектор на Android). Хранилищ два:
 * без квалификатора — токены и настройки (на iOS — Keychain), [DeviceCache] — кэши устройства.
 */
expect val platformNetworkModule: Module

/**
 * Кэши устройства, которые растут вместе с данными (таблица id домашнего сервера). Отдельно от
 * токенов: SharedPreferences читает файл целиком при первом обращении, и большой кэш тормозил бы
 * старт; а на iOS кэшу не место в Keychain.
 */
val DeviceCache: Qualifier = named("device_cache")

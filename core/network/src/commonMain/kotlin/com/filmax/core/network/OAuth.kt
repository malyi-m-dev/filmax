package com.filmax.core.network

/**
 * Константы OAuth-эндпоинта kino.pub — единый источник для авторизации (`:data:auth`)
 * и авто-обновления токена в [buildHttpClient]. Держим здесь, чтобы обмен refresh_token
 * в сетевом слое не зависел от `:data:auth` (иначе циклическая зависимость модулей).
 */
const val OAUTH_DEVICE_PATH = "api/oauth2/device"
const val OAUTH_CLIENT_ID = "android"
const val OAUTH_CLIENT_SECRET = "rcaqh7wodackn9ll1uggvqkx2iib6umh"

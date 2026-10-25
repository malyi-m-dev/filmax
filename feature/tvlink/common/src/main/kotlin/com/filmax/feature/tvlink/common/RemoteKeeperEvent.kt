package com.filmax.feature.tvlink.common

sealed interface RemoteKeeperEvent {
    /**
     * Filmax на экране телефона и вошёл в аккаунт — держать связь с запомненным ТВ. Свернули или
     * вышли из аккаунта — [Hidden]: повторы подключения останавливаются.
     */
    data object Shown : RemoteKeeperEvent
    data object Hidden : RemoteKeeperEvent
}

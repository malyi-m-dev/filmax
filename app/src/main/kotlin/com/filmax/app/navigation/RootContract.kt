package com.filmax.app.navigation

data class RootState(
    /** null — статус ещё не определён (сплэш); true/false — авторизован или нет. */
    val isAuthenticated: Boolean? = null,
    /** Инициалы пользователя для аватара в TV-таб-баре (пусто — гость/не загружено). */
    val initials: String = "",
)

sealed interface RootEvent

sealed interface RootSideEffect

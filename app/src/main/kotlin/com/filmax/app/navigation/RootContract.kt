package com.filmax.app.navigation

data class RootState(
    /** null — статус ещё не определён (сплэш); true/false — авторизован или нет. */
    val isAuthenticated: Boolean? = null,
)

sealed interface RootEvent

sealed interface RootSideEffect

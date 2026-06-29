package com.filmax.tv.navigation

data class TvRootState(
    /** null — статус ещё не определён (сплэш); true/false — авторизован или нет. */
    val isAuthenticated: Boolean? = null,
)

sealed interface TvRootEvent

sealed interface TvRootSideEffect

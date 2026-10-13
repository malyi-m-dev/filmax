package com.filmax.app.navigation

data class RootState(

    /** null — статус ещё не определён (сплэш); true/false — авторизован или нет. */

    val isAuthenticated: Boolean? = null,

    /** Аккаунт для бокового меню ТВ: аватар и карточка профиля. Пустой — гость или не загружен. */

    val account: AccountSummary = AccountSummary(),

)

/** Что меню показывает об аккаунте — готовые строки, вёрстка их только рисует. */

data class AccountSummary(

    val name: String = "",

    val subscription: String = "",

    val initials: String = "",

)

sealed interface RootEvent

sealed interface RootSideEffect

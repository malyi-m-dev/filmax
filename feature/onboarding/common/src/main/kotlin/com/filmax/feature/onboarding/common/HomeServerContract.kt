package com.filmax.feature.onboarding.common

import com.filmax.core.domain.source.HomeServer
import com.filmax.core.presentation.input.KeyboardLayout
import com.filmax.core.presentation.input.keyboardRows

/** Шаг входа на домашний сервер. */
enum class HomeServerStep {
    /** Выбор сервера: найденные в сети или адрес вручную. */
    SERVER,

    /** Код Quick Connect: вход подтверждают в Jellyfin на телефоне или в браузере. */
    CODE,

    /** Имя пользователя и пароль. */
    PASSWORD,
}

/** Поле, в которое печатает экранная клавиатура ТВ. */
enum class HomeServerField { ADDRESS, USERNAME, PASSWORD }

data class HomeServerState(
    val step: HomeServerStep = HomeServerStep.SERVER,
    /** Идёт поиск серверов в локальной сети. */
    val searching: Boolean = false,
    val found: List<HomeServer> = emptyList(),
    val address: String = "",
    /** Сервер, к которому подключились: на шагах входа — его имя и адрес. */
    val server: HomeServer? = null,
    val username: String = "",
    val password: String = "",
    val field: HomeServerField = HomeServerField.ADDRESS,
    val layout: KeyboardLayout = KeyboardLayout.EN,
    /** Код Quick Connect для показа; null — ещё не выдан. */
    val code: String? = null,
    /** Идёт подключение или вход — кнопка действия показывает ожидание. */
    val busy: Boolean = false,
    val error: String? = null,
)

/** Нажатие экранной клавиатуры ТВ. */
sealed interface ServerKey {
    data class Letter(val key: String) : ServerKey
    data object Space : ServerKey
    data object Erase : ServerKey
    data class Layout(val layout: KeyboardLayout) : ServerKey
}

sealed interface HomeServerEvent {
    /** Поискать серверы в сети ещё раз. */
    data object Search : HomeServerEvent
    data class Choose(val server: HomeServer) : HomeServerEvent

    /** Подключиться по введённому адресу. */
    data object Connect : HomeServerEvent

    data class SetAddress(val text: String) : HomeServerEvent
    data class SetUsername(val text: String) : HomeServerEvent
    data class SetPassword(val text: String) : HomeServerEvent

    /** ТВ: клавиатура печатает в это поле. */
    data class Focus(val field: HomeServerField) : HomeServerEvent
    data class Key(val input: ServerKey) : HomeServerEvent

    data object SignIn : HomeServerEvent
    data object UseCode : HomeServerEvent
    data object UsePassword : HomeServerEvent
    data object ChangeServer : HomeServerEvent
}

sealed interface HomeServerSideEffect {
    /** Вход выполнен, источник — домашний сервер: экран уводит на Главную. */
    data object SignedIn : HomeServerSideEffect
}

/**
 * Ряды клавиатуры для адреса, имени и пароля: в цифровой раскладке — ещё и символы адреса
 * («.», «:», «/»), без них адрес сервера пультом не набрать.
 */
fun serverKeyboardRows(layout: KeyboardLayout): List<List<String>> = when (layout) {
    KeyboardLayout.DIGITS -> listOf("1234567890", ".:/-_@").map { row -> row.map(Char::toString) }
    else -> keyboardRows(layout)
}

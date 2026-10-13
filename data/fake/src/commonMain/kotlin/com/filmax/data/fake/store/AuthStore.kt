package com.filmax.data.fake.store

import com.filmax.core.domain.auth.model.DeviceCode
import com.filmax.core.domain.auth.model.Token
import com.filmax.data.fake.FakeIds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet

/** Адрес активации — тот же, что онбординг показывает по умолчанию. */
private const val VERIFICATION_URI = "kinopub.me/device"

/** Сколько живёт код: с запасом, чтобы онбординг не успел упереться в таймаут. */
private const val CODE_LIFETIME_SECONDS = 600

/** Интервал опроса, который онбординг выдерживает между запросами токена. */
private const val POLL_INTERVAL_SECONDS = 1

/** На каком опросе код «подтверждается»: при интервале в секунду вход занимает ~5 секунд. */
private const val POLLS_UNTIL_APPROVED = 5

/** Срок жизни выданного токена — сутки, как у kino.pub. */
private const val TOKEN_LIFETIME_SECONDS = 86_400

private val MOCK_TOKEN = Token(
    accessToken = "mock-access-token",
    refreshToken = "mock-refresh-token",
    expiresIn = TOKEN_LIFETIME_SECONDS,
)

/** Выданный код и сколько раз его уже опросили. [serial] делает каждый код уникальным. */
private data class PendingCode(val code: String, val polls: Int, val serial: Int)

/**
 * Вход device-flow без человека: код «подтверждается» сам через несколько опросов. Этого хватает,
 * чтобы разглядеть экран с кодом, и не нужно никуда идти с телефоном. Выход — просто флаг: данные
 * аккаунта (закладки, прогресс) переживают повторный вход, как на сервере.
 */
internal class AuthStore(authenticated: Boolean) {

    private val authenticatedState = MutableStateFlow(authenticated)
    private val pending = MutableStateFlow(PendingCode(code = "", polls = 0, serial = 0))

    val isAuthenticated: StateFlow<Boolean> = authenticatedState.asStateFlow()

    /** Новый код сбрасывает счётчик опросов: «Повторить» на онбординге начинает ожидание заново. */
    fun issueDeviceCode(): DeviceCode {
        val issued = pending.updateAndGet { current ->
            val serial = current.serial + 1
            PendingCode(code = "mock-device-code-$serial", polls = 0, serial = serial)
        }
        return DeviceCode(
            code = issued.code,
            userCode = FakeIds.DEVICE_USER_CODE,
            verificationUri = VERIFICATION_URI,
            expiresIn = CODE_LIFETIME_SECONDS,
            interval = POLL_INTERVAL_SECONDS,
        )
    }

    /** null — код ещё «не подтверждён» или устарел; токен — вход состоялся. */
    fun poll(code: String): Token? {
        val polled = pending.updateAndGet { current ->
            if (current.code == code) current.copy(polls = current.polls + 1) else current
        }
        if (polled.code != code || polled.polls < POLLS_UNTIL_APPROVED) return null
        authenticatedState.value = true
        return MOCK_TOKEN
    }

    /** Обмен refresh_token всегда удаётся: протухания сессии в мок-режиме нет. */
    fun refresh(): Token = MOCK_TOKEN

    fun logout() {
        authenticatedState.value = false
    }
}

package com.filmax.data.fake

import com.filmax.core.domain.auth.AuthRepository
import com.filmax.core.domain.auth.model.DeviceCode
import com.filmax.core.domain.auth.model.Token
import com.filmax.core.domain.common.RequestResult
import kotlinx.coroutines.flow.Flow

/**
 * Вход без kino.pub: сессия открыта с первого кадра, а после «Выйти» онбординг проходится сам —
 * код подтверждается через ~5 секунд опросов. Так проверяется весь круг «выход → онбординг →
 * вход» без человека с телефоном.
 */
class FakeAuthRepository(private val backend: FakeBackend) : AuthRepository {

    override val isAuthenticated: Flow<Boolean> = backend.auth.isAuthenticated

    override suspend fun requestDeviceCode(): RequestResult<DeviceCode> =
        backend.respond { success(auth.issueDeviceCode()) }

    // Без задержки сети: паузу между опросами и так держит онбординг (interval), а лишние
    // полсекунды на каждый опрос растянули бы «вход» с пяти секунд до восьми.
    override suspend fun pollForToken(code: String, username: String, timestamp: Long): RequestResult<Token> =
        backend.auth.poll(code)?.let { token -> success(token) } ?: authorizationPending()

    override suspend fun refreshToken(refreshToken: String): RequestResult<Token> =
        backend.respond { success(auth.refresh()) }

    override suspend fun logout(): RequestResult<Unit> = backend.respond {
        auth.logout()
        success(Unit)
    }
}

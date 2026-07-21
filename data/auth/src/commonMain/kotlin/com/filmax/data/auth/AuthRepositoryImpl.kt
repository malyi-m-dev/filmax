package com.filmax.data.auth

import com.filmax.core.domain.auth.AuthRepository
import com.filmax.core.domain.auth.model.DeviceCode
import com.filmax.core.domain.auth.model.Token
import com.filmax.core.domain.common.ErrorReporting
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.common.safeRequest
import com.filmax.core.network.TokenStorage
import com.filmax.data.auth.remote.AuthApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class AuthRepositoryImpl(
    private val api: AuthApi,
    private val tokenStorage: TokenStorage,
) : AuthRepository {

    override val isAuthenticated: Flow<Boolean> =
        tokenStorage.accessToken.map { it != null }

    override suspend fun requestDeviceCode(): RequestResult<DeviceCode> = safeRequest {
        val dto = api.requestDeviceCode()
        DeviceCode(
            code = dto.code,
            userCode = dto.userCode,
            verificationUri = dto.verificationUri,
            expiresIn = dto.expiresIn,
            interval = dto.interval,
        )
    }

    override suspend fun pollForToken(code: String, username: String, timestamp: Long): RequestResult<Token> =
        safeRequest {
            val dto = api.pollForToken(code = code, username = username, timestamp = timestamp)
            tokenStorage.save(dto.accessToken, dto.refreshToken)
            Token(dto.accessToken, dto.refreshToken, dto.expiresIn)
        }

    override suspend fun refreshToken(refreshToken: String): RequestResult<Token> = safeRequest {
        val dto = api.refreshToken(refreshToken = refreshToken)
        tokenStorage.save(dto.accessToken, dto.refreshToken)
        Token(dto.accessToken, dto.refreshToken, dto.expiresIn)
    }

    override suspend fun logout(): RequestResult<Unit> = safeRequest {
        tokenStorage.clear()
        // Дальше устройством может пользоваться другой аккаунт — отвязываем телеметрию.
        ErrorReporting.reporter.setUser(null)
    }
}

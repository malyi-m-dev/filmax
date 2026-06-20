package com.filmax.data.auth

import com.filmax.core.domain.auth.AuthRepository
import com.filmax.core.domain.auth.model.DeviceCode
import com.filmax.core.domain.auth.model.Token
import com.filmax.core.network.TokenStorage
import com.filmax.data.auth.remote.AuthApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val tokenStorage: TokenStorage,
) : AuthRepository {

    override val isAuthenticated: Flow<Boolean> =
        tokenStorage.accessToken.map { it != null }

    override suspend fun requestDeviceCode(): DeviceCode {
        val dto = api.requestDeviceCode()
        return DeviceCode(
            code            = dto.code,
            userCode        = dto.userCode,
            verificationUri = dto.verificationUri,
            expiresIn       = dto.expiresIn,
            interval        = dto.interval,
        )
    }

    override suspend fun pollForToken(code: String, username: String, timestamp: Long): Token {
        val dto = api.pollForToken(code = code, username = username, timestamp = timestamp)
        tokenStorage.save(dto.accessToken, dto.refreshToken)
        return Token(dto.accessToken, dto.refreshToken, dto.expiresIn)
    }

    override suspend fun refreshToken(refreshToken: String): Token {
        val dto = api.refreshToken(refreshToken = refreshToken)
        tokenStorage.save(dto.accessToken, dto.refreshToken)
        return Token(dto.accessToken, dto.refreshToken, dto.expiresIn)
    }

    override suspend fun logout() {
        tokenStorage.clear()
    }
}

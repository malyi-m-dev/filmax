package com.filmax.core.domain.auth

import com.filmax.core.domain.auth.model.DeviceCode
import com.filmax.core.domain.auth.model.Token
import com.filmax.core.domain.common.RequestResult
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    val isAuthenticated: Flow<Boolean>

    suspend fun requestDeviceCode(): RequestResult<DeviceCode>

    suspend fun pollForToken(code: String, username: String, timestamp: Long): RequestResult<Token>

    suspend fun refreshToken(refreshToken: String): RequestResult<Token>

    suspend fun logout(): RequestResult<Unit>
}

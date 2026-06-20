package com.filmax.core.domain.auth

import com.filmax.core.domain.auth.model.DeviceCode
import com.filmax.core.domain.auth.model.Token
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    val isAuthenticated: Flow<Boolean>

    suspend fun requestDeviceCode(): DeviceCode

    suspend fun pollForToken(code: String, username: String, timestamp: Long): Token

    suspend fun refreshToken(refreshToken: String): Token

    suspend fun logout()
}

package com.filmax.core.domain.usecase.auth

import com.filmax.core.domain.auth.AuthRepository
import com.filmax.core.domain.auth.model.DeviceCode
import com.filmax.core.domain.auth.model.Token
import com.filmax.core.domain.common.RequestResult
import kotlinx.coroutines.flow.Flow

/**
 * Общий контракт OAuth device-flow для Android и iOS.
 * Тонкие UseCase поверх [AuthRepository] — единая точка входа для обоих presentation-слоёв.
 */

class ObserveAuthStateUseCase(private val repository: AuthRepository) {
    operator fun invoke(): Flow<Boolean> = repository.isAuthenticated
}

class RequestDeviceCodeUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(): RequestResult<DeviceCode> = repository.requestDeviceCode()
}

class PollForTokenUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(code: String, username: String, timestamp: Long): RequestResult<Token> =
        repository.pollForToken(code, username, timestamp)
}

class LogoutUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(): RequestResult<Unit> = repository.logout()
}

package com.filmax.data.jellyfin

import com.filmax.core.domain.auth.AuthRepository
import com.filmax.core.domain.auth.model.DeviceCode
import com.filmax.core.domain.auth.model.Token
import com.filmax.core.domain.common.RequestResult
import kotlinx.coroutines.flow.Flow

/**
 * Вход и выход на домашнем сервере в общем контракте авторизации. Код устройства — это вход
 * kino.pub; на сервер входят по адресу и паролю или коду Quick Connect (HomeServerRepository),
 * и сюда из этого контракта приходят только признак входа и выход.
 */
internal class JellyfinAuthRepository(private val context: JellyfinContext) : AuthRepository {

    override val isAuthenticated: Flow<Boolean> = context.session.signedIn

    override suspend fun requestDeviceCode(): RequestResult<DeviceCode> = RequestResult.Error(NO_DEVICE_CODE)

    override suspend fun pollForToken(code: String, username: String, timestamp: Long): RequestResult<Token> =
        RequestResult.Error(NO_DEVICE_CODE)

    override suspend fun refreshToken(refreshToken: String): RequestResult<Token> = RequestResult.Error(NO_DEVICE_CODE)

    /** Токен гасится и на сервере, но выйти можно и без сети: сессия на устройстве стирается всегда. */
    override suspend fun logout(): RequestResult<Unit> {
        context.request { _ -> context.api.logout() }
        context.session.clear()
        return RequestResult.Success(Unit)
    }

    private companion object {
        const val NO_DEVICE_CODE = "На домашний сервер входят по адресу и паролю"
    }
}

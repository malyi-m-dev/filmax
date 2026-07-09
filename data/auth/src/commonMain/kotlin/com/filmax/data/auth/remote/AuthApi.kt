package com.filmax.data.auth.remote

import com.filmax.core.network.OAUTH_CLIENT_ID
import com.filmax.core.network.OAUTH_CLIENT_SECRET
import com.filmax.core.network.OAUTH_DEVICE_PATH
import com.filmax.data.auth.remote.dto.DeviceCodeDto
import com.filmax.data.auth.remote.dto.TokenDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.request.post

internal class AuthApi(private val client: HttpClient) {

    suspend fun requestDeviceCode(): DeviceCodeDto =
        client.post(OAUTH_DEVICE_PATH) {
            parameter("grant_type", "device_code")
            parameter("client_id", OAUTH_CLIENT_ID)
            parameter("client_secret", OAUTH_CLIENT_SECRET)
        }.body()

    suspend fun pollForToken(code: String, username: String, timestamp: Long): TokenDto =
        client.post(OAUTH_DEVICE_PATH) {
            parameter("grant_type", "device_token")
            parameter("client_id", OAUTH_CLIENT_ID)
            parameter("client_secret", OAUTH_CLIENT_SECRET)
            parameter("code", code)
            parameter("username", username)
            parameter("timestamp", timestamp)
        }.body()

    suspend fun refreshToken(refreshToken: String): TokenDto =
        client.post(OAUTH_DEVICE_PATH) {
            parameter("grant_type", "refresh_token")
            parameter("client_id", OAUTH_CLIENT_ID)
            parameter("client_secret", OAUTH_CLIENT_SECRET)
            parameter("refresh_token", refreshToken)
        }.body()
}

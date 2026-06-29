package com.filmax.data.auth.remote

import com.filmax.data.auth.remote.dto.DeviceCodeDto
import com.filmax.data.auth.remote.dto.TokenDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.request.post

internal class AuthApi(private val client: HttpClient) {

    suspend fun requestDeviceCode(): DeviceCodeDto =
        client.post("api/oauth2/device") {
            parameter("grant_type", "device_code")
            parameter("client_id", CLIENT_ID)
            parameter("client_secret", CLIENT_SECRET)
        }.body()

    suspend fun pollForToken(code: String, username: String, timestamp: Long): TokenDto =
        client.post("api/oauth2/device") {
            parameter("grant_type", "device_token")
            parameter("client_id", CLIENT_ID)
            parameter("client_secret", CLIENT_SECRET)
            parameter("code", code)
            parameter("username", username)
            parameter("timestamp", timestamp)
        }.body()

    suspend fun refreshToken(refreshToken: String): TokenDto =
        client.post("api/oauth2/device") {
            parameter("grant_type", "refresh_token")
            parameter("client_id", CLIENT_ID)
            parameter("client_secret", CLIENT_SECRET)
            parameter("refresh_token", refreshToken)
        }.body()

    private companion object {
        const val CLIENT_ID = "android"
        const val CLIENT_SECRET = "rcaqh7wodackn9ll1uggvqkx2iib6umh"
    }
}

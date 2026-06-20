package com.filmax.data.auth.remote

import com.filmax.data.auth.remote.dto.DeviceCodeDto
import com.filmax.data.auth.remote.dto.TokenDto
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApi {

    @POST("api/oauth2/device")
    suspend fun requestDeviceCode(
        @Query("grant_type")    grantType: String    = "device_code",
        @Query("client_id")     clientId: String     = CLIENT_ID,
        @Query("client_secret") clientSecret: String = CLIENT_SECRET,
    ): DeviceCodeDto

    @POST("api/oauth2/device")
    suspend fun pollForToken(
        @Query("grant_type")    grantType: String    = "device_token",
        @Query("client_id")     clientId: String     = CLIENT_ID,
        @Query("client_secret") clientSecret: String = CLIENT_SECRET,
        @Query("code")          code: String,
        @Query("username")      username: String,
        @Query("timestamp")     timestamp: Long,
    ): TokenDto

    @POST("api/oauth2/device")
    suspend fun refreshToken(
        @Query("grant_type")     grantType: String    = "refresh_token",
        @Query("client_id")      clientId: String     = CLIENT_ID,
        @Query("client_secret")  clientSecret: String = CLIENT_SECRET,
        @Query("refresh_token")  refreshToken: String,
    ): TokenDto

    companion object {
        const val CLIENT_ID     = "android"
        const val CLIENT_SECRET = "rcaqh7wodackn9ll1uggvqkx2iib6umh"
    }
}

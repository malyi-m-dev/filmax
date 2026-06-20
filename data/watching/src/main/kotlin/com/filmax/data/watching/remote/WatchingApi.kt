package com.filmax.data.watching.remote

import com.filmax.data.watching.remote.dto.HistoryResponseDto
import com.filmax.data.watching.remote.dto.NotificationsDto
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface WatchingApi {

    @GET("api/v1/watching/{type}")
    suspend fun getHistory(
        @Path("type")        type: String = "all",
        @Query("subscribed") subscribed: Int = 0,
    ): HistoryResponseDto

    @GET("api/v1/watching/marktime")
    suspend fun saveProgress(
        @Query("id")    id: Int,
        @Query("video") video: Int,
        @Query("time")  time: Int,
    )

    @GET("api/v1/watching/marktime")
    suspend fun saveProgressSerial(
        @Query("id")     id: Int,
        @Query("season") season: Int,
        @Query("video")  video: Int,
        @Query("time")   time: Int,
    )

    @GET("api/v1/watching/toggle")
    suspend fun toggleWatched(@Query("id") id: Int)

    @GET("api/v1/watching/togglewatchlist")
    suspend fun toggleWatchlist(@Query("id") id: Int): Map<String, Int>

    @GET("api/v1/history/clear-for-item")
    suspend fun clearItemHistory(@Query("id") id: Int)

    @GET("api2/v1.1/notifications")
    suspend fun getNotifications(): NotificationsDto

    @FormUrlEncoded
    @POST("api2/v1.1/notifications/read")
    suspend fun markNotificationRead(@Field("id") id: Int)

    @POST("api2/v1.1/notifications/read-all")
    suspend fun markAllNotificationsRead()
}

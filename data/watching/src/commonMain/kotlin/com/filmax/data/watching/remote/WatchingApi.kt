package com.filmax.data.watching.remote

import com.filmax.data.watching.remote.dto.HistoryResponseDto
import com.filmax.data.watching.remote.dto.NotificationsDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.Parameters

internal class WatchingApi(private val client: HttpClient) {

    suspend fun getHistory(type: String = "all", subscribed: Int = 0): HistoryResponseDto =
        client.get("api/v1/watching/$type") {
            parameter("subscribed", subscribed)
        }.body()

    suspend fun saveProgress(id: Int, video: Int, time: Int) {
        client.get("api/v1/watching/marktime") {
            parameter("id", id)
            parameter("video", video)
            parameter("time", time)
        }
    }

    suspend fun saveProgressSerial(id: Int, season: Int, video: Int, time: Int) {
        client.get("api/v1/watching/marktime") {
            parameter("id", id)
            parameter("season", season)
            parameter("video", video)
            parameter("time", time)
        }
    }

    suspend fun toggleWatched(id: Int) {
        client.get("api/v1/watching/toggle") { parameter("id", id) }
    }

    suspend fun toggleWatchlist(id: Int): Map<String, Int> =
        client.get("api/v1/watching/togglewatchlist") { parameter("id", id) }.body()

    suspend fun clearItemHistory(id: Int) {
        client.get("api/v1/history/clear-for-item") { parameter("id", id) }
    }

    suspend fun getNotifications(): NotificationsDto =
        client.get("api2/v1.1/notifications").body()

    suspend fun markNotificationRead(id: Int) {
        client.submitForm(
            url = "api2/v1.1/notifications/read",
            formParameters = Parameters.build { append("id", id.toString()) },
        )
    }

    suspend fun markAllNotificationsRead() {
        client.post("api2/v1.1/notifications/read-all")
    }
}

package com.filmax.data.watching.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HistoryResponseDto(
    val items: List<HistoryItemDto>,
    val pagination: PaginationDto? = null,
)

@Serializable
data class HistoryItemDto(
    val id: Int,
    val title: String,
    val type: String = "",
    val posters: PostersDto? = null,
    val watching: WatchProgressDto? = null,
)

@Serializable
data class WatchProgressDto(
    val status: Int = 0,
    val time: Int? = null,
    val duration: Int? = null,
    val video: Int? = null,
    val season: Int? = null,
)

@Serializable
data class PostersDto(
    val small: String = "",
    val medium: String = "",
    val big: String = "",
)

@Serializable
data class PaginationDto(
    val total: Int = 0,
    val current: Int = 1,
    @SerialName("per_page") val perPage: Int = 20,
)

@Serializable
data class NotificationsDto(
    val notifications: List<NotificationDto>? = null,
    val unread: Int = 0,
)

@Serializable
data class NotificationDto(
    val id: Int,
    val title: String? = null,
    val text: String? = null,
    @SerialName("created_at") val createdAt: Int? = null,
    val read: Boolean = false,
    val type: String? = null,
    @SerialName("item_id") val itemId: Int? = null,
)

package com.filmax.data.user.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountInfoDto(
    val user: UserDto? = null,
    val subscription: SubscriptionDto? = null,
)

@Serializable
data class UserDto(
    val id: Int,
    val username: String,
    val email: String? = null,
    val avatar: String? = null,
    @SerialName("reg_date") val regDate: Int? = null,
)

@Serializable
data class SubscriptionDto(
    val active: Boolean,
    val end: Int? = null,
    val days: Int? = null,
)

@Serializable
data class DeviceSettingsDto(
    val device: DeviceInfoDto? = null,
)

@Serializable
data class DeviceInfoDto(
    val id: Int,
    val title: String,
    val hardware: String? = null,
    val software: String? = null,
    @SerialName("support_ssl")   val supportSsl: Int = 0,
    @SerialName("support_hevc")  val supportHevc: Int = 0,
    @SerialName("support_hdr")   val supportHdr: Int = 0,
    @SerialName("support_4k")    val support4k: Int = 0,
    @SerialName("mixed_playlist") val mixedPlaylist: Int = 0,
    @SerialName("streaming_type") val streamingType: Int = 0,
    @SerialName("server_location") val serverLocation: Int = 0,
)

@Serializable
data class BookmarksDto(
    val items: List<BookmarkFolderDto>,
)

@Serializable
data class BookmarkFolderDto(
    val id: Int,
    val title: String,
    val count: Int = 0,
    @SerialName("updated_at") val updatedAt: Int? = null,
)

@Serializable
data class FolderStatusDto(
    val status: Int,
    val id: Int? = null,
)

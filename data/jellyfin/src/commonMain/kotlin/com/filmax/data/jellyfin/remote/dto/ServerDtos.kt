// Ответы сервера о себе, о входе и о воспроизведении. Nullable по той же причине, что и ItemDtos.
package com.filmax.data.jellyfin.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PublicSystemInfoDto(
    @SerialName("ServerName") val serverName: String? = null,
    @SerialName("Version") val version: String? = null,
    @SerialName("Id") val id: String? = null,
)

@Serializable
internal data class UserDto(
    @SerialName("Id") val id: String? = null,
    @SerialName("Name") val name: String? = null,
    @SerialName("PrimaryImageTag") val primaryImageTag: String? = null,
)

@Serializable
internal data class AuthenticationResultDto(
    @SerialName("User") val user: UserDto? = null,
    @SerialName("AccessToken") val accessToken: String? = null,
)

@Serializable
internal data class QuickConnectResultDto(
    @SerialName("Authenticated") val authenticated: Boolean? = null,
    @SerialName("Secret") val secret: String? = null,
    @SerialName("Code") val code: String? = null,
)

/** Ответ на широковещательный запрос поиска сервера (UDP 7359). */
@Serializable
internal data class DiscoveryResponseDto(
    @SerialName("Address") val address: String? = null,
    @SerialName("Id") val id: String? = null,
    @SerialName("Name") val name: String? = null,
)

@Serializable
internal data class PlaybackInfoResponseDto(
    @SerialName("MediaSources") val mediaSources: List<MediaSourceDto>? = null,
    @SerialName("PlaySessionId") val playSessionId: String? = null,
    @SerialName("ErrorCode") val errorCode: String? = null,
)

@Serializable
internal data class MediaSourceDto(
    @SerialName("Id") val id: String? = null,
    @SerialName("SupportsDirectPlay") val supportsDirectPlay: Boolean? = null,
    @SerialName("TranscodingUrl") val transcodingUrl: String? = null,
    @SerialName("RunTimeTicks") val runTimeTicks: Long? = null,
    @SerialName("MediaStreams") val mediaStreams: List<MediaStreamDto>? = null,
)

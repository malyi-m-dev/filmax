// Тела запросов к серверу. Значения по умолчанию уходят в запрос (encodeDefaults), null — нет.
package com.filmax.data.jellyfin.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AuthenticateByNameBody(
    @SerialName("Username") val username: String,
    @SerialName("Pw") val password: String,
)

@Serializable
internal data class QuickConnectBody(@SerialName("Secret") val secret: String)

@Serializable
internal data class PlaybackInfoBody(
    @SerialName("UserId") val userId: String,
    @SerialName("MaxStreamingBitrate") val maxStreamingBitrate: Long,
    @SerialName("DeviceProfile") val deviceProfile: DeviceProfileDto,
    @SerialName("EnableDirectPlay") val enableDirectPlay: Boolean,
    @SerialName("EnableDirectStream") val enableDirectStream: Boolean,
    @SerialName("EnableTranscoding") val enableTranscoding: Boolean = true,
    @SerialName("AllowVideoStreamCopy") val allowVideoStreamCopy: Boolean = true,
    @SerialName("AllowAudioStreamCopy") val allowAudioStreamCopy: Boolean = true,
    @SerialName("AutoOpenLiveStream") val autoOpenLiveStream: Boolean = true,
)

@Serializable
internal data class DeviceProfileDto(
    @SerialName("Name") val name: String,
    @SerialName("MaxStreamingBitrate") val maxStreamingBitrate: Long,
    @SerialName("MaxStaticBitrate") val maxStaticBitrate: Long,
    @SerialName("DirectPlayProfiles") val directPlayProfiles: List<DirectPlayProfileDto>,
    @SerialName("TranscodingProfiles") val transcodingProfiles: List<TranscodingProfileDto>,
    @SerialName("SubtitleProfiles") val subtitleProfiles: List<SubtitleProfileDto>,
    @SerialName("CodecProfiles") val codecProfiles: List<String> = emptyList(),
    @SerialName("ContainerProfiles") val containerProfiles: List<String> = emptyList(),
)

@Serializable
internal data class DirectPlayProfileDto(
    @SerialName("Container") val container: String,
    @SerialName("VideoCodec") val videoCodec: String,
    @SerialName("AudioCodec") val audioCodec: String,
    @SerialName("Type") val type: String = "Video",
)

@Serializable
internal data class TranscodingProfileDto(
    @SerialName("Container") val container: String,
    @SerialName("VideoCodec") val videoCodec: String,
    @SerialName("AudioCodec") val audioCodec: String,
    @SerialName("Protocol") val protocol: String,
    @SerialName("MaxAudioChannels") val maxAudioChannels: String,
    @SerialName("Type") val type: String = "Video",
    @SerialName("Context") val context: String = "Streaming",
    @SerialName("MinSegments") val minSegments: Int = 1,
    @SerialName("BreakOnNonKeyFrames") val breakOnNonKeyFrames: Boolean = true,
)

@Serializable
internal data class SubtitleProfileDto(
    @SerialName("Format") val format: String,
    @SerialName("Method") val method: String,
)

/** Прогресс просмотра: и старт, и тики — одно тело (PlaybackStartInfo и PlaybackProgressInfo). */
@Serializable
internal data class PlaybackProgressBody(
    @SerialName("ItemId") val itemId: String,
    @SerialName("PositionTicks") val positionTicks: Long,
    @SerialName("CanSeek") val canSeek: Boolean = true,
    @SerialName("IsPaused") val isPaused: Boolean = false,
)

@Serializable
internal data class UserDataBody(
    @SerialName("PlaybackPositionTicks") val playbackPositionTicks: Long? = null,
    @SerialName("Played") val played: Boolean? = null,
)

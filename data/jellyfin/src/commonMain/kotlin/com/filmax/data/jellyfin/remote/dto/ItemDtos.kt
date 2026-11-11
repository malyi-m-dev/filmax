// Ответы Jellyfin о тайтлах. Все поля nullable с умолчанием: сервер пропускает поля, которых у
// элемента нет (у фильма — сезона, у серии — фона), а одно обязательное поле уронило бы разбор
// всей страницы.
package com.filmax.data.jellyfin.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ItemsResponseDto(
    @SerialName("Items") val items: List<ItemDto>? = null,
    @SerialName("TotalRecordCount") val totalRecordCount: Int? = null,
    @SerialName("StartIndex") val startIndex: Int? = null,
)

@Serializable
internal data class ItemDto(
    @SerialName("Id") val id: String? = null,
    @SerialName("Name") val name: String? = null,
    @SerialName("Type") val type: String? = null,
    @SerialName("ProductionYear") val productionYear: Int? = null,
    @SerialName("Overview") val overview: String? = null,
    @SerialName("GenreItems") val genreItems: List<NamedIdDto>? = null,
    @SerialName("CommunityRating") val communityRating: Double? = null,
    @SerialName("RunTimeTicks") val runTimeTicks: Long? = null,
    @SerialName("ImageTags") val imageTags: Map<String, String>? = null,
    @SerialName("BackdropImageTags") val backdropImageTags: List<String>? = null,
    @SerialName("ParentBackdropItemId") val parentBackdropItemId: String? = null,
    @SerialName("ParentBackdropImageTags") val parentBackdropImageTags: List<String>? = null,
    @SerialName("SeriesId") val seriesId: String? = null,
    @SerialName("SeriesName") val seriesName: String? = null,
    @SerialName("SeriesPrimaryImageTag") val seriesPrimaryImageTag: String? = null,
    @SerialName("IndexNumber") val indexNumber: Int? = null,
    @SerialName("ParentIndexNumber") val parentIndexNumber: Int? = null,
    @SerialName("UserData") val userData: UserDataDto? = null,
    @SerialName("People") val people: List<PersonDto>? = null,
    @SerialName("ProductionLocations") val productionLocations: List<String>? = null,
    @SerialName("ProviderIds") val providerIds: Map<String, String?>? = null,
    @SerialName("Status") val status: String? = null,
    @SerialName("RemoteTrailers") val remoteTrailers: List<TrailerDto>? = null,
    @SerialName("MediaStreams") val mediaStreams: List<MediaStreamDto>? = null,
    @SerialName("Width") val width: Int? = null,
    @SerialName("Height") val height: Int? = null,
)

@Serializable
internal data class NamedIdDto(
    @SerialName("Name") val name: String? = null,
    @SerialName("Id") val id: String? = null,
)

@Serializable
internal data class UserDataDto(
    @SerialName("PlaybackPositionTicks") val playbackPositionTicks: Long? = null,
    @SerialName("PlayCount") val playCount: Int? = null,
    @SerialName("IsFavorite") val isFavorite: Boolean? = null,
    @SerialName("Played") val played: Boolean? = null,
    @SerialName("LastPlayedDate") val lastPlayedDate: String? = null,
)

@Serializable
internal data class PersonDto(
    @SerialName("Name") val name: String? = null,
    @SerialName("Role") val role: String? = null,
    @SerialName("Type") val type: String? = null,
)

@Serializable
internal data class TrailerDto(
    @SerialName("Url") val url: String? = null,
    @SerialName("Name") val name: String? = null,
)

@Serializable
internal data class MediaStreamDto(
    @SerialName("Type") val type: String? = null,
    @SerialName("Index") val index: Int? = null,
    @SerialName("Codec") val codec: String? = null,
    @SerialName("Language") val language: String? = null,
    @SerialName("Title") val title: String? = null,
    @SerialName("DisplayTitle") val displayTitle: String? = null,
    @SerialName("Channels") val channels: Int? = null,
    @SerialName("Width") val width: Int? = null,
    @SerialName("Height") val height: Int? = null,
    @SerialName("DeliveryMethod") val deliveryMethod: String? = null,
    @SerialName("DeliveryUrl") val deliveryUrl: String? = null,
)
